package com.songshawn.nutritrack.data.network

import com.songshawn.nutritrack.data.network.fruityvice.FruitDto
import com.songshawn.nutritrack.data.network.genai.OpenAIRequest
import com.songshawn.nutritrack.data.network.genai.OpenAIResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

/** OpenAI-compatible chat completions API (Zhipu AI). */
interface GenAIApiService {
    @POST("chat/completions")
    suspend fun generateContent(
        @Body request: OpenAIRequest,
        @Header("Authorization") authorization: String,
    ): Response<OpenAIResponse>

    /** SSE streaming variant; returns the raw body so it can be read line by line. */
    @Streaming
    @POST("chat/completions")
    suspend fun generateContentStream(
        @Body request: OpenAIRequest,
        @Header("Authorization") authorization: String,
    ): Response<ResponseBody>
}

interface FruityviceApi {
    @GET("api/fruit/{name}")
    suspend fun getFruitByName(@Path("name") name: String): Response<FruitDto>
}
