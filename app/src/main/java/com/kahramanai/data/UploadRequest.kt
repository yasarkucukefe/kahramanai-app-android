package com.kahramanai.data

data class UploadRequest (
    val cid: Int?,
    val dnm: Int,
    val uuid: String,
    val desc: String,
    val size: Long?,
    val ext: String,
    val pdf: Int,
    val contentType: String
)

