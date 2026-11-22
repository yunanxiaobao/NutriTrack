package com.songshawn.nutritrack.data.repository

import com.google.gson.GsonBuilder
import com.songshawn.nutritrack.data.network.GenAIApiService
import com.songshawn.nutritrack.viewmodel.ApiResult
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SseStreamingMockWebServerTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: GenAIRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
            .create(GenAIApiService::class.java)

        repository = GenAIRepository(api).apply {
            bearerTokenProvider = { "Bearer mock-firebase-id-token" }
        }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `streamText parses multi-chunk SSE frames and skips malformed frames until DONE`() = runBlocking {
        val ssePayload =
            """
            data: {"id":"1","choices":[{"index":0,"delta":{"role":"assistant","content":"Eat "}}]}

            data: {MALFORMED_JSON_FRAME_SHOULD_BE_SKIPPED}

            data: {"id":"1","choices":[{"index":0,"delta":{"content":"more "}}]}

            data: {"id":"1","choices":[{"index":0,"delta":{"content":"leafy greens."},"finish_reason":"stop"}]}

            data: [DONE]
            """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/event-stream")
                .setBody(ssePayload),
        )

        val events = repository.streamText("Give me a vegetable tip").toList()
        val recordedRequest = server.takeRequest()

        assertEquals("Bearer mock-firebase-id-token", recordedRequest.getHeader("Authorization"))
        assertEquals("/chat/completions", recordedRequest.path)

        val chunks = events.filterIsInstance<StreamResult.Chunk>()
        assertEquals("Eat more leafy greens.", chunks.joinToString("") { it.text })
        assertTrue(events.last() is StreamResult.Complete)
    }

    @Test
    fun `streamText emits StreamResult Error on HTTP 429 rate-limit response`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("""{"error":"Rate limit exceeded"}"""),
        )

        val events = repository.streamText("Hello").toList()
        assertEquals(1, events.size)
        val error = events.first() as StreamResult.Error
        assertTrue(error.message.contains("429"))
    }

    @Test
    fun `generateText parses single-shot completion and maps HTTP 500 to ApiResult Error Http`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"choices":[{"message":{"role":"assistant","content":"Drink 1.5L water daily."}}]}"""),
        )
        val okResult = repository.generateText("Hydration tip")
        assertTrue(okResult is ApiResult.Success)
        assertEquals("Drink 1.5L water daily.", (okResult as ApiResult.Success).data)

        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"error":"Upstream error"}"""),
        )
        val errResult = repository.generateText("Hydration tip")
        assertTrue(errResult is ApiResult.Error.Http)
        assertEquals(500, (errResult as ApiResult.Error.Http).code)
    }
}
