package com.kahramanai.network

import com.kahramanai.data.PresignedUrlResponse
import com.kahramanai.data.ResponseJwtBundle
import com.kahramanai.data.UploadRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Url

interface ApiService {

    @Multipart
    @POST
    suspend fun uploadFileToS3(
        @Url url: String,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part
    ): Response<Unit> // S3 returns a 204 No Content on success, so Response<Unit> is appropriate.

    // JWT get presigned
    @POST("upload2_presigned")
    suspend fun route_jwt_presigned(
        @Header("Authorization") authToken: String,
        @Body requestBody: UploadRequest
    ): Response<PresignedUrlResponse>

    // JWT code init
    @POST("jwt_bundle")
    suspend fun route_jwt_bundle(
        @Header("Authorization") authToken: String
    ): Response<ResponseJwtBundle>
}