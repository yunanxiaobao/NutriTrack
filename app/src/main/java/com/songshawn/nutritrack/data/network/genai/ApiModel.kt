package com.songshawn.nutritrack.data.network.genai

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

// OpenAI-compatible chat/completions wire models (used with Zhipu AI GLM-4).

data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val stream: Boolean = false,
    val tools: List<OpenAIToolDefinition>? = null,
    @SerializedName("tool_choice") val toolChoice: String? = null,
)

data class OpenAIMessage(
    val role: String,
    /** Either a plain `String`, `List<ContentPart>` for multimodal input, or `null` when emitting tool_calls. */
    val content: Any? = null,
    @SerializedName("tool_calls") val toolCalls: List<OpenAIToolCall>? = null,
    @SerializedName("tool_call_id") val toolCallId: String? = null,
    val name: String? = null,
)

data class OpenAIToolDefinition(
    val type: String = "function",
    val function: OpenAIFunctionSpec,
)

data class OpenAIFunctionSpec(
    val name: String,
    val description: String,
    val parameters: JsonObject,
)

data class OpenAIToolCall(
    val id: String,
    val type: String = "function",
    val function: OpenAIFunctionCall,
)

data class OpenAIFunctionCall(
    val name: String,
    val arguments: String,
)

data class ContentPart(
    val type: String,
    val text: String? = null,
    @SerializedName("image_url") val imageUrl: ImageUrl? = null,
)

data class ImageUrl(
    val url: String,
)

data class OpenAIResponse(
    val choices: List<OpenAIChoice>?,
)

data class OpenAIChoice(
    val message: OpenAIMessageContent,
    @SerializedName("finish_reason") val finishReason: String? = null,
)

data class OpenAIMessageContent(
    val role: String,
    val content: String? = null,
    @SerializedName("tool_calls") val toolCalls: List<OpenAIToolCall>? = null,
)

/**
 * One SSE frame of a streaming response, e.g.
 * `data: {"id":"...","choices":[{"delta":{"content":"text"}}]}`
 */
data class OpenAIStreamResponse(
    val id: String? = null,
    val choices: List<OpenAIStreamChoice>? = null,
)

data class OpenAIStreamChoice(
    val index: Int = 0,
    val delta: OpenAIDelta? = null,
    @SerializedName("finish_reason") val finishReason: String? = null,
)

data class OpenAIDelta(
    val role: String? = null,
    val content: String? = null,
)
