package com.songshawn.nutritrack.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.songshawn.nutritrack.BuildConfig
import com.songshawn.nutritrack.data.network.FruityviceApi
import com.songshawn.nutritrack.data.network.GenAIApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val DIRECT_ZHIPU_BASE_URL = "https://open.bigmodel.cn/api/paas/v4/"
    private const val FRUITYVICE_BASE_URL = "https://www.fruityvice.com/"

    private fun resolveLlmBaseUrl(): String {
        val proxyUrl = BuildConfig.LLM_PROXY_BASE_URL.trim()
        if (proxyUrl.isEmpty()) return DIRECT_ZHIPU_BASE_URL
        return if (proxyUrl.endsWith("/")) proxyUrl else "$proxyUrl/"
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().serializeNulls().create()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            // HEADERS, not BODY: BODY buffers the whole response, which defeats SSE
            // streaming, and would dump base64 images and prompts into logcat.
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
                redactHeader("Authorization")
            }
            builder.addInterceptor(logging)
        }
        return builder.build()
    }

    @Provides
    @Singleton
    @Named("ZhipuRetrofit")
    fun provideZhipuRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(resolveLlmBaseUrl())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    @Named("FruityviceRetrofit")
    fun provideFruityviceRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(FRUITYVICE_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun provideGenAIApiService(@Named("ZhipuRetrofit") retrofit: Retrofit): GenAIApiService =
        retrofit.create(GenAIApiService::class.java)

    @Provides
    @Singleton
    fun provideFruityviceApi(@Named("FruityviceRetrofit") retrofit: Retrofit): FruityviceApi =
        retrofit.create(FruityviceApi::class.java)
}
