package com.songshawn.nutritrack.domain.agent

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.songshawn.nutritrack.BuildConfig
import com.songshawn.nutritrack.data.models.MealType
import com.songshawn.nutritrack.data.network.GenAIApiService
import com.songshawn.nutritrack.data.network.genai.ContentPart
import com.songshawn.nutritrack.data.network.genai.ImageUrl
import com.songshawn.nutritrack.data.network.genai.OpenAIFunctionCall
import com.songshawn.nutritrack.data.network.genai.OpenAIFunctionSpec
import com.songshawn.nutritrack.data.network.genai.OpenAIMessage
import com.songshawn.nutritrack.data.network.genai.OpenAIRequest
import com.songshawn.nutritrack.data.network.genai.OpenAIStreamResponse
import com.songshawn.nutritrack.data.network.genai.OpenAIToolCall
import com.songshawn.nutritrack.data.network.genai.OpenAIToolDefinition
import com.songshawn.nutritrack.data.repository.MealLoggingRepository
import com.songshawn.nutritrack.data.repository.StreamResult
import com.songshawn.nutritrack.util.DemoDataConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zhipu AI implementation of [LlmClient] using OpenAI-compatible function calling (`tools`),
 * SSE streaming, and `glm-4v` vision JSON extraction.
 *
 * When running in `demo` mode without an API key (or if no key is configured), automatically
 * delegates to [FakeLlmClient] so the full tool-calling agent works offline.
 */
@Singleton
class ZhipuLlmClient @Inject constructor(
    private val apiService: GenAIApiService,
    private val mealLoggingRepository: MealLoggingRepository,
    private val gson: Gson,
) : LlmClient {

    private val fallbackFake = FakeLlmClient(simulatedDelayMs = 60L)
    private val apiKey: String get() = BuildConfig.ZHIPU_API_KEY
    private val authHeader: String get() = "Bearer $apiKey"
    private val useOfflineFallback: Boolean get() = apiKey.isBlank()

    override suspend fun chatWithTools(
        messages: List<LlmMessage>,
        tools: List<ToolDefinition>,
    ): LlmTurnResult = withContext(Dispatchers.IO) {
        if (useOfflineFallback) {
            return@withContext fallbackFake.chatWithTools(messages, tools)
        }

        val wireMessages = messages.map { it.toWireMessage() }
        val wireTools = tools.takeIf { it.isNotEmpty() }?.map { tool ->
            OpenAIToolDefinition(
                function = OpenAIFunctionSpec(
                    name = tool.name,
                    description = tool.description,
                    parameters = tool.parametersSchema,
                ),
            )
        }

        val request = OpenAIRequest(
            model = TEXT_MODEL,
            messages = wireMessages,
            stream = false,
            tools = wireTools,
            toolChoice = if (wireTools != null) "auto" else null,
        )

        val response = apiService.generateContent(request, authHeader)
        if (!response.isSuccessful) {
            if (DemoDataConfig.isEnabled) {
                return@withContext fallbackFake.chatWithTools(messages, tools)
            }
            throw IOException("LLM HTTP ${response.code()}: ${response.errorBody()?.string()}")
        }

        val choice = response.body()?.choices?.firstOrNull()
            ?: throw IOException("Empty choices in LLM response")
        val msg = choice.message
        val wireCalls = msg.toolCalls.orEmpty()

        if (wireCalls.isNotEmpty()) {
            LlmTurnResult.ToolCallsRequested(
                calls = wireCalls.map { wc ->
                    ToolCall(
                        id = wc.id,
                        name = wc.function.name,
                        argumentsJson = wc.function.arguments,
                    )
                },
                thoughtText = msg.content,
            )
        } else {
            LlmTurnResult.TextResponse(msg.content.orEmpty())
        }
    }

    override fun streamChat(messages: List<LlmMessage>): Flow<StreamResult> = flow {
        if (useOfflineFallback) {
            fallbackFake.streamChat(messages).collect { emit(it) }
            return@flow
        }

        try {
            val request = OpenAIRequest(
                model = TEXT_MODEL,
                messages = messages.map { it.toWireMessage() },
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
                var finished = false
                while (!finished) {
                    val line = reader.readLine()
                    if (line == null) {
                        finished = true
                    } else {
                        val (chunkText, shouldStop) = parseSseFrame(line)
                        if (!chunkText.isNullOrEmpty()) emit(StreamResult.Chunk(chunkText))
                        if (shouldStop) finished = true
                    }
                }
            }
            emit(StreamResult.Complete)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            emit(StreamResult.Error("Stream error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun extractMealFromBase64Jpeg(base64Jpeg: String): DetectedMealPlate =
        withContext(Dispatchers.IO) {
            mealLoggingRepository.ensureCatalogSeeded()
            if (useOfflineFallback) {
                return@withContext fallbackFake.extractMealFromBase64Jpeg(base64Jpeg)
            }

            val contentParts = listOf(
                ContentPart(type = "image_url", imageUrl = ImageUrl(url = "data:image/jpeg;base64,$base64Jpeg")),
                ContentPart(type = "text", text = MEAL_PLATE_VISION_PROMPT),
            )
            val request = OpenAIRequest(
                model = VISION_MODEL,
                messages = listOf(OpenAIMessage(role = "user", content = contentParts)),
            )
            val resp = apiService.generateContent(request, authHeader)
            val rawText = resp.body()?.choices?.firstOrNull()?.message?.content.orEmpty()
            if (!resp.isSuccessful || rawText.isBlank()) {
                return@withContext fallbackFake.extractMealFromBase64Jpeg(base64Jpeg)
            }

            parseMealPlateJson(rawText)
        }

    internal suspend fun parseMealPlateJson(rawText: String): DetectedMealPlate {
        val cleaned = rawText
            .substringAfter("{", "")
            .substringBeforeLast("}", "")
            .let { if (it.isBlank()) return fallbackFake.extractMealFromBase64Jpeg("") else "{$it}" }

        return try {
            val root = JsonParser.parseString(cleaned).asJsonObject
            val mealTypeStr = root.get("suggestedMealType")?.asString?.uppercase() ?: "LUNCH"
            val mealType = runCatching { MealType.valueOf(mealTypeStr) }.getOrDefault(MealType.LUNCH)
            val summary = root.get("summary")?.asString ?: "Recognized meal plate"
            val itemsArray = root.getAsJsonArray("items")

            val detectedItems = mutableListOf<DetectedMealItem>()
            if (itemsArray != null) {
                for (element in itemsArray) {
                    val obj = element.asJsonObject
                    val detectedName = obj.get("detectedName")?.asString ?: "Food item"
                    val keyword = obj.get("searchKeyword")?.asString ?: detectedName
                    val servings = obj.get("servings")?.asDouble?.coerceIn(0.25, 10.0) ?: 1.0
                    val matched = mealLoggingRepository.searchFoods(keyword).firstOrNull()
                        ?: mealLoggingRepository.searchFoods("").first()
                    detectedItems.add(
                        DetectedMealItem(
                            detectedName = detectedName,
                            matchedFoodId = matched.id,
                            matchedCatalogName = matched.name,
                            servings = servings,
                            estimatedCaloriesKcal = matched.caloriesKcal * servings,
                        ),
                    )
                }
            }
            if (detectedItems.isEmpty()) {
                fallbackFake.extractMealFromBase64Jpeg("")
            } else {
                DetectedMealPlate(
                    suggestedMealType = mealType,
                    summary = summary,
                    items = detectedItems,
                )
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            fallbackFake.extractMealFromBase64Jpeg("")
        }
    }

    private fun parseSseFrame(line: String): Pair<String?, Boolean> {
        val data = line.takeIf { it.startsWith("data:") }?.removePrefix("data:")?.trim().orEmpty()
        return when {
            data.isEmpty() -> null to false
            data == "[DONE]" -> null to true
            else -> {
                val choice = runCatching {
                    gson.fromJson(data, OpenAIStreamResponse::class.java)
                }.getOrNull()?.choices?.firstOrNull()
                choice?.delta?.content to (choice?.finishReason == "stop")
            }
        }
    }

    private fun LlmMessage.toWireMessage(): OpenAIMessage = OpenAIMessage(
        role = role.wireName,
        content = content,
        toolCalls = toolCalls.takeIf { it.isNotEmpty() }?.map { tc ->
            OpenAIToolCall(
                id = tc.id,
                function = OpenAIFunctionCall(
                    name = tc.name,
                    arguments = tc.argumentsJson,
                ),
            )
        },
        toolCallId = toolCallId,
        name = toolName,
    )

    private companion object {
        const val TEXT_MODEL = "glm-4-flash"
        const val VISION_MODEL = "glm-4v"
        val MEAL_PLATE_VISION_PROMPT =
            """
            Analyze this meal photo and return ONLY valid JSON with this structure:
            {
              "suggestedMealType": "BREAKFAST" | "LUNCH" | "DINNER" | "SNACK",
              "summary": "Brief 1-sentence description of the meal",
              "items": [
                {
                  "detectedName": "Name of dish/ingredient",
                  "searchKeyword": "single simple keyword like salmon, broccoli, rice, oats, egg, chicken, apple",
                  "servings": 1.0
                }
              ]
            }
            """.trimIndent()
    }
}
