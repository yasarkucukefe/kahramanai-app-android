package com.kahramanai.data

data class PresignedUrlResponse(
    val url: String,
    val fields: Map<String, String>,
    val dnm: Int
)

data class ResponseJwtBundle(
    val auto_id: Int,
    val bid: Int,
    val bundle_code: String,
    val bundle_name: String,
    val customer_name: String
)

