package com.kahramanai.util

sealed class NetworkResult<T>(
    val data: T? = null,
    val message: String? = null,
    val code: Int? = null
) {
    class Success<T>(data: T) : NetworkResult<T>(data)
    class Error<T>(code: Int, message: String, data: T? = null) : NetworkResult<T>(data, message, code)
    class Loading<T> : NetworkResult<T>()
}