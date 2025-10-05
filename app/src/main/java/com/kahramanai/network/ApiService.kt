package com.kahramanai.network

import com.kahramanai.data.CheckShareToken
import com.kahramanai.data.PostRequestBid
import com.kahramanai.data.PresignedUrlResponse
import com.kahramanai.data.ResponseJwtBundle
import com.kahramanai.data.ShrBundle
import com.kahramanai.data.ShrCustomer
import com.kahramanai.data.UploadRequest
import com.kahramanai.data.UserCredits
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

    // Upload file to S3
    @Multipart
    @POST
    suspend fun uploadFileToS3(
        @Url url: String,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part
    ): Response<Unit> // S3 returns a 204 No Content on success, so Response<Unit> is appropriate.

    // SHARE TOKEN rountes

    // Check share token
    @POST("shr_check_token")
    suspend fun route_shared_check(
        @Header("Authorization") authToken: String
    ): Response<CheckShareToken>

    @POST("shr_customer")
    suspend fun route_shared_customer(
        @Header("Authorization") authToken: String
    ): Response<ShrCustomer>

    @POST("shr_bundle")
    suspend fun route_shared_bundle(
        @Header("Authorization") authToken: String,
        @Body requestBody: PostRequestBid
    ): Response<ShrBundle>

    @POST("shr_bundles")
    suspend fun route_shared_bundle_list(
        @Header("Authorization") authToken: String
    ): Response<List<ShrBundle>>

    @POST("shr_userCredits")
    suspend fun route_shared_userCredits(
        @Header("Authorization") authToken: String
    ): Response<UserCredits>

    @POST("shr_generate_upload_url")
    suspend fun routeSharedGenerateUploadUrl(
        @Header("Authorization") authToken: String,
        @Body requestBody: UploadRequest
    ): Response<PresignedUrlResponse>


    // JWT get presigned
    @POST("upload2_presigned")
    suspend fun route_jwt_presigned(
        @Header("Authorization") authToken: String,
        @Body requestBody: UploadRequest
    ): Response<PresignedUrlResponse>

    // JWT user credits
    @POST("jwt_userCredits")
    suspend fun route_jwt_userCredits(
        @Header("Authorization") authToken: String
    ): Response<UserCredits>

    // JWT code init
    @POST("jwt_bundle")
    suspend fun route_jwt_bundle(
        @Header("Authorization") authToken: String
    ): Response<ResponseJwtBundle>
}