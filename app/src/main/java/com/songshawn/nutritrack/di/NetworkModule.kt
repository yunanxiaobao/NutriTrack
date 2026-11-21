package com.songshawn.nutritrack.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
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

    private const val GEMINI_BASE_URL = "https://open.bigmodel.cn/api/paas/v4/"
    private const val FRUITYVICE_BASE_URL = "https://www.fruityvice.com/"

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder().serializeNulls().create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("GeminiRetrofit")
    fun provideGeminiRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(GEMINI_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @Named("FruityviceRetrofit")
    fun provideFruityviceRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(FRUITYVICE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideGenAIApiService(@Named("GeminiRetrofit") retrofit: Retrofit): GenAIApiService {
        return retrofit.create(GenAIApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideFruityviceApi(@Named("FruityviceRetrofit") retrofit: Retrofit): FruityviceApi {
        return retrofit.create(FruityviceApi::class.java)
    }
}
