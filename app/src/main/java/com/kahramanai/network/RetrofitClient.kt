package com.kahramanai.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private val BASE_URL = "https://kahramanai.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL) // 1. Set the base URL
            .client(okHttpClient) // 2. Set the custom OkHttp client
            .addConverterFactory(GsonConverterFactory.create()) // 3. Set the JSON converter
            .build()

        // 4. Create an implementation of the ApiService interface
        // This single instance can handle all methods defined in ApiService
        retrofit.create(ApiService::class.java)
    }
}