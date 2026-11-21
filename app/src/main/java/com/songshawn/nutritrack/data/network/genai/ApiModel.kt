package com.songshawn.nutritrack.data.network.genai

import com.google.gson.annotations.SerializedName

// OpenAI-compatible chat/completions wire models (used with Zhipu AI).

data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val stream: Boolean = false,
)

data class OpenAIMessage(
    val role: String,
    /** Either a plain `String` or a `List<ContentPart>` for multimodal input. */
    val content: Any,
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
)

data class OpenAIMessageContent(
    val role: String,
    val content: String,
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
