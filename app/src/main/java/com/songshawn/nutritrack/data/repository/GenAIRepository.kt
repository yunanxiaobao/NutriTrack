package com.songshawn.nutritrack.data.repository

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.songshawn.nutritrack.BuildConfig
import com.songshawn.nutritrack.data.network.GenAIApiService
import com.songshawn.nutritrack.data.network.genai.ContentPart
import com.songshawn.nutritrack.data.network.genai.ImageUrl
import com.songshawn.nutritrack.data.network.genai.OpenAIMessage
import com.songshawn.nutritrack.data.network.genai.OpenAIRequest
import com.songshawn.nutritrack.data.network.genai.OpenAIStreamResponse
import com.songshawn.nutritrack.viewmodel.ApiResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject

/**
 * Talks to Zhipu AI's OpenAI-compatible `chat/completions` endpoint.
 *
 * Note: the API key is still read from BuildConfig here. Moving it behind a
 * server-side proxy is tracked as a separate change (see docs/roadmap.md).
 */
class GenAIRepository @Inject constructor(
    private val apiService: GenAIApiService,
) {
    private val gson = Gson()
    private val authHeader: String get() = "Bearer ${BuildConfig.ZHIPU_API_KEY}"

    /** Single-shot text completion. */
    suspend fun generateText(prompt: String): ApiResult<String> = withContext(Dispatchers.IO) {
        runCatchingApi {
            val request = OpenAIRequest(
                model = TEXT_MODEL,
                messages = listOf(OpenAIMessage(role = "user", content = prompt)),
            )
            val resp = apiService.generateContent(request, authHeader)
            if (!resp.isSuccessful) {
                return@runCatchingApi ApiResult.Error.Http(
                    code = resp.code(),
                    message = resp.message(),
                    body = resp.errorBody()?.string(),
                )
            }
            val text = resp.body()?.choices?.firstOrNull()?.message?.content
                ?: return@runCatchingApi ApiResult.Error.Parsing(
                    IllegalStateException("empty choices/message/content"),
                )
            ApiResult.Success(text)
        }
    }

    /**
     * Streams a text completion via Server-Sent Events.
     *
     * Emits [StreamResult.Chunk] for each delta, then exactly one terminal
     * [StreamResult.Complete] or [StreamResult.Error]. Cancellation of the
     * collector propagates and closes the underlying HTTP stream.
     */
    fun streamText(prompt: String): Flow<StreamResult> = flow {
        try {
            val request = OpenAIRequest(
                model = TEXT_MODEL,
                messages = listOf(OpenAIMessage(role = "user", content = prompt)),
                stream = true,
            )
            val response = apiService.generateContentStream(request, authHeader)
            if (!response.isSuccessful) {
                emit(StreamResult.Error("HTTP ${response.code()}: ${response.errorBody()?.string()}"))
                return@flow
            }
            val body = response.body()
            if (body == null) {
                emit(StreamResult.Error("Empty response body"))
                return@flow
            }

            // `use` guarantees the connection is released even if the collector is cancelled.
            body.byteStream().bufferedReader().use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data.isEmpty()) continue
                    if (data == "[DONE]") break

                    val chunk = try {
                        gson.fromJson(data, OpenAIStreamResponse::class.java)
                    } catch (e: com.google.gson.JsonSyntaxException) {
                        continue // skip a malformed frame rather than aborting the whole answer
                    }
                    val choice = chunk.choices?.firstOrNull()
                    val content = choice?.delta?.content
                    if (!content.isNullOrEmpty()) emit(StreamResult.Chunk(content))
                    if (choice?.finishReason == "stop") break
                }
            }
            emit(StreamResult.Complete)
        } catch (ce: CancellationException) {
            throw ce
        } catch (io: IOException) {
            emit(StreamResult.Error("Network error: ${io.message}"))
        } catch (t: Throwable) {
            emit(StreamResult.Error("Error: ${t.message}"))
        }
    }.flowOn(Dispatchers.IO)

    /** Asks the vision model to name the fruit in [bitmap]. */
    suspend fun identifyFruitFromImage(bitmap: Bitmap): ApiResult<String> = withContext(Dispatchers.IO) {
        runCatchingApi {
            val contentParts = listOf(
                ContentPart(type = "image_url", imageUrl = ImageUrl(url = "data:image/jpeg;base64,${bitmap.toBase64Jpeg()}")),
                ContentPart(type = "text", text = FRUIT_PROMPT),
            )
            val request = OpenAIRequest(
                model = VISION_MODEL,
                messages = listOf(OpenAIMessage(role = "user", content = contentParts)),
            )
            val resp = apiService.generateContent(request, authHeader)
            if (!resp.isSuccessful) {
                return@runCatchingApi ApiResult.Error.Http(
                    code = resp.code(),
                    message = resp.message(),
                    body = resp.errorBody()?.string(),
                )
            }
            val text = resp.body()?.choices?.firstOrNull()?.message?.content?.trim()
                ?: return@runCatchingApi ApiResult.Error.Parsing(IllegalStateException("empty response"))
            ApiResult.Success(text)
        }
    }

    private inline fun <T> runCatchingApi(block: () -> ApiResult<T>): ApiResult<T> = try {
        block()
    } catch (ce: CancellationException) {
        throw ce
    } catch (io: IOException) {
        ApiResult.Error.Network(io)
    } catch (t: Throwable) {
        ApiResult.Error.Unknown(t)
    }

    private fun Bitmap.toBase64Jpeg(quality: Int = 80): String {
        val out = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, quality, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private companion object {
        /** Free tier text model. */
        const val TEXT_MODEL = "glm-4-flash"

        /** Stable vision model; the one this feature was tested against. */
        const val VISION_MODEL = "glm-4v"

        const val FRUIT_PROMPT =
            "Identify this fruit and return only the fruit name in English. If it's not a fruit, return 'Not a fruit'."
    }
}

/** Result of a streaming AI response. */
sealed class StreamResult {
    /** An incremental piece of the answer. */
    data class Chunk(val text: String) : StreamResult()

    /** The stream finished normally. */
    data object Complete : StreamResult()

    /** The stream failed; no further items will be emitted. */
    data class Error(val message: String) : StreamResult()
}
