package com.songshawn.nutritrack.domain.agent

import com.google.gson.JsonObject
import com.songshawn.nutritrack.data.models.MealType
import com.songshawn.nutritrack.data.repository.StreamResult
import kotlinx.coroutines.flow.Flow

enum class LlmRole(val wireName: String) {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool"),
}

data class LlmMessage(
    val role: LlmRole,
    val content: String? = null,
    val toolCalls: List<ToolCall> = emptyList(),
    val toolCallId: String? = null,
    val toolName: String? = null,
)

data class ToolDefinition(
    val name: String,
    val description: String,
    val parametersSchema: JsonObject,
    /** Whether invoking this tool mutates user state and must await explicit user confirmation. */
    val requiresConfirmation: Boolean = false,
)

data class ToolCall(
    val id: String,
    val name: String,
    val argumentsJson: String,
)

sealed class LlmTurnResult {
    data class TextResponse(val text: String) : LlmTurnResult()
    data class ToolCallsRequested(
        val calls: List<ToolCall>,
        val thoughtText: String? = null,
    ) : LlmTurnResult()
}

/**
 * Structured output from analyzing a meal plate photo.
 */
data class DetectedMealPlate(
    val suggestedMealType: MealType,
    val summary: String,
    val items: List<DetectedMealItem>,
)

data class DetectedMealItem(
    val detectedName: String,
    val matchedFoodId: String,
    val matchedCatalogName: String,
    val servings: Double,
    val estimatedCaloriesKcal: Double,
)

/**
 * Provider-agnostic LLM interface supporting single-shot chat, SSE streaming,
 * OpenAI-style tool calling, and multimodal meal-plate extraction.
 */
interface LlmClient {
    suspend fun chatWithTools(
        messages: List<LlmMessage>,
        tools: List<ToolDefinition>,
    ): LlmTurnResult

    fun streamChat(messages: List<LlmMessage>): Flow<StreamResult>

    suspend fun extractMealFromBase64Jpeg(base64Jpeg: String): DetectedMealPlate
}
