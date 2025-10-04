package com.kahramanai.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kahramanai.data.PresignedUrlResponse
import com.kahramanai.data.ResponseJwtBundle
import com.kahramanai.util.NetworkResult
import com.kahramanai.data.UploadRequest
import com.kahramanai.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class MainViewModel : ViewModel() {

    // StateFlow to track the number of pending uploads ---
    // Private mutable state that only the ViewModel can change
    private val _pendingUploads = MutableStateFlow(0)
    // Public, read-only state for the UI to observe
    val pendingUploads: StateFlow<Int> = _pendingUploads.asStateFlow()

    // Route jwt_bundle
    private val _postResult1 = MutableLiveData<NetworkResult<ResponseJwtBundle>>()
    val postResult1: LiveData<NetworkResult<ResponseJwtBundle>> = _postResult1

    private val _postResult2 = MutableLiveData<NetworkResult<PresignedUrlResponse>>()
    val postResult2: LiveData<NetworkResult<PresignedUrlResponse>> = _postResult2

    private val _postResult3 = MutableLiveData<NetworkResult<Unit>>()
    val postResult3: LiveData<NetworkResult<Unit>> = _postResult3

    fun uploadFileS3(presignedUrlResponse: PresignedUrlResponse, file: File) {
        // This updates the UI as soon as the user triggers the upload.
        _pendingUploads.value++

        viewModelScope.launch {
            _postResult3.postValue(NetworkResult.Loading()) // Notify UI that we are loading
            try {

                // 1. Convert the fields map to a RequestBody map
                val fieldsMap = presignedUrlResponse.fields.mapValues {
                    it.value.toRequestBody("text/plain".toMediaTypeOrNull())
                }

                // 2. Create the MultipartBody.Part for the file
                // Get the Content-Type from the presigned response to ensure it's correct
                val contentType = presignedUrlResponse.fields["Content-Type"]
                val fileRequestBody = file.asRequestBody(contentType?.toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", file.name, fileRequestBody)

                // Call the new API method. Note there is no request body object.
                val response = RetrofitClient.instance.uploadFileToS3(
                    url = presignedUrlResponse.url,
                    fields = fieldsMap,
                    file = filePart)

                if (response.isSuccessful) {
                    _postResult3.postValue(NetworkResult.Success(Unit))
                } else {
                    // Send a detailed error state
                    val errorMsg = response.errorBody()?.string() ?: "Unknown S3 Error"
                    println("S3 Error: ${errorMsg}")
                    _postResult3.postValue(NetworkResult.Error(response.code(), errorMsg))
                }
            } catch (e: Exception) {
                // Send a generic exception state
                println("S3 Network Error Error: ${e.message}")
                _postResult3.postValue(NetworkResult.Error(0, e.message ?: "An exception occurred"))
            } finally {
                // Decrement the counter when the upload is complete ---
                // The 'finally' block ensures this code runs whether the upload
                // succeeded, failed, or threw an exception.
                _pendingUploads.value--
            }
        }
    }

    fun routeJWTupload2_presigned(jwt: String?, postData: UploadRequest) {
        viewModelScope.launch {
            _postResult2.postValue(NetworkResult.Loading()) // Notify UI that we are loading
            try {
                val authToken = "Bearer $jwt"

                // Call the new API method. Note there is no request body object.
                val response = RetrofitClient.instance.route_jwt_presigned(authToken, postData)

                if (response.isSuccessful) {
                    _postResult2.postValue(NetworkResult.Success(response.body()!!))
                } else {
                    // Send a detailed error state
                    val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                    _postResult2.postValue(NetworkResult.Error(response.code(), errorMsg))
                }
            } catch (e: Exception) {
                // Send a generic exception state
                _postResult2.postValue(NetworkResult.Error(0, e.message ?: "An exception occurred"))
            }
        }
    }

    fun routeJWTbundle(jwt: String) {
        viewModelScope.launch {
            _postResult1.postValue(NetworkResult.Loading()) // Notify UI that we are loading
            try {
                val authToken = "Bearer $jwt"

                // Call the new API method. Note there is no request body object.
                val response = RetrofitClient.instance.route_jwt_bundle(authToken)

                if (response.isSuccessful) {
                    _postResult1.postValue(NetworkResult.Success(response.body()!!))
                } else {
                    // Send a detailed error state
                    val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                    _postResult1.postValue(NetworkResult.Error(response.code(), errorMsg))
                }
            } catch (e: Exception) {
                // Send a generic exception state
                _postResult1.postValue(NetworkResult.Error(0, e.message ?: "An exception occurred"))
            }
        }
    }

}