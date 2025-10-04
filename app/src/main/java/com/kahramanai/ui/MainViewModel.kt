package com.kahramanai.ui

import android.R
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kahramanai.data.ResponseJwtBundle
import com.kahramanai.util.NetworkResult
import com.kahramanai.data.UploadRequest
import com.kahramanai.network.RetrofitClient
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // Route jwt_bundle
    private val _postResult1 = MutableLiveData<NetworkResult<ResponseJwtBundle>>()
    val postResult1: LiveData<NetworkResult<ResponseJwtBundle>> = _postResult1

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