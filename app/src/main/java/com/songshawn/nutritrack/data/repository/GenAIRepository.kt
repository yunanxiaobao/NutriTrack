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
import com.songshawn.nutritrack.util.DemoDataConfig
import com.songshawn.nutritrack.viewmodel.ApiResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject

/**
 * Talks to Zhipu AI's OpenAI-compatible `chat/completions` endpoint, with an offline
 * simulation fallback in the `demo` product flavor when no API key is configured.
 */
class GenAIRepository @Inject constructor(
    private val apiService: GenAIApiService,
) {
    private val gson = Gson()
    private val apiKey: String get() = BuildConfig.ZHIPU_API_KEY
    private val authHeader: String get() = "Bearer $apiKey"
    private val useDemoOfflineLlm: Boolean get() = DemoDataConfig.isEnabled && apiKey.isBlank()

    /** Single-shot text completion. */
    suspend fun generateText(prompt: String): ApiResult<String> = withContext(Dispatchers.IO) {
        if (useDemoOfflineLlm) {
            delay(180L)
            return@withContext ApiResult.Success(buildDemoCoachingResponse(prompt))
        }
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
     * Streams a text completion via Server-Sent Events (or simulated chunks in offline demo mode).
     */
    fun streamText(prompt: String): Flow<StreamResult> = flow {
        if (useDemoOfflineLlm) {
            val fullText = buildDemoCoachingResponse(prompt)
            val words = fullText.split(" ")
            for (word in words) {
                delay(20L)
                emit(StreamResult.Chunk("$word "))
            }
            emit(StreamResult.Complete)
            return@flow
        }

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

            body.byteStream().bufferedReader().use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data.isEmpty()) continue
                    if (data == "[DONE]") break

                    val chunk = try {
                        gson.fromJson(data, OpenAIStreamResponse::class.java)
                    } catch (_: com.google.gson.JsonSyntaxException) {
                        continue
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
        if (useDemoOfflineLlm) {
            delay(200L)
            return@withContext ApiResult.Success("Banana")
        }
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

    private fun buildDemoCoachingResponse(prompt: String): String =
        if (prompt.contains("clinician", ignoreCase = true) || prompt.contains("cohort", ignoreCase = true)) {
            "1. **Vegetable Variety Gap**: Across the cohort, quantity reaches ~65% of target while " +
                "sub-category diversity lags; rotating dark-green, cruciferous, and legume serves yields the " +
                "fastest HEIFA gain.\n" +
                "2. **Wholegrain Substitution**: Participants replacing refined grains with rolled oats or " +
                "brown rice average +4.2 points higher total HEIFA scores.\n" +
                "3. **Hydration & Discretionary Moderation**: Keeping discretionary treats at <=1 serve/day " +
                "and water >=1.5L strongly correlates with top-quartile scores (>80/100)."
        } else {
            "### Personalized NutriCoach Tip (Demo Mode)\n" +
                "- **Strong Whole-Food Foundation**: Your logged meals show consistent wholegrain oats, " +
                "colorful vegetables (spinach, broccoli, carrots, lentils), and omega-3 rich salmon.\n" +
                "- **Next +5 Point Opportunity**: Add one extra half-cup of legumes or dark-green vegetables " +
                "at lunch and keep daily water intake at 3 glasses (1,500 mL) to lock in a 90+ HEIFA score."
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
        const val TEXT_MODEL = "glm-4-flash"
        const val VISION_MODEL = "glm-4v"
        const val FRUIT_PROMPT =
            "Identify this fruit and return only the fruit name in English. If it's not a fruit, return 'Not a fruit'."
    }
}

/** Result of a streaming AI response. */
sealed class StreamResult {
    data class Chunk(val text: String) : StreamResult()
    data object Complete : StreamResult()
    data class Error(val message: String) : StreamResult()
}
