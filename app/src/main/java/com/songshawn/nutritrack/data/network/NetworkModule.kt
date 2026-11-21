package com.songshawn.nutritrack.data.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.songshawn.nutritrack.data.network.fruityvice.FruitDto
import com.songshawn.nutritrack.data.network.genai.OpenAIRequest
import com.songshawn.nutritrack.data.network.genai.OpenAIResponse
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit

interface GenAIApiService {
    // DeepSeek / OpenAI Compatible Endpoint
    @POST("chat/completions")
    suspend fun generateContent(
        @Body request: OpenAIRequest,
        @Header("Authorization") authorization: String
    ): Response<OpenAIResponse>

    // SSE Streaming Endpoint - returns raw ResponseBody for streaming
    @Streaming
    @POST("chat/completions")
    suspend fun generateContentStream(
        @Body request: OpenAIRequest,
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>
}

interface FruityviceApi {
    @GET("api/fruit/{name}")
    suspend fun getFruitByName(@Path("name") name: String): Response<FruitDto>
}
