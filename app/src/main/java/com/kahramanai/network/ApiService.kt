package com.kahramanai.network

import com.kahramanai.data.PresignedUrlResponse
import com.kahramanai.data.ResponseJwtBundle
import com.kahramanai.data.UploadRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    // JWT code init
    @POST("jwt_bundle")
    suspend fun route_jwt_bundle(
        @Header("Authorization") authToken: String
    ): Response<ResponseJwtBundle>
}