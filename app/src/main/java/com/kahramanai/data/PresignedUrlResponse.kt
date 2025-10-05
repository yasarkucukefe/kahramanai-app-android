package com.kahramanai.data
import com.google.gson.annotations.SerializedName

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

data class UserCredits(
    val credits: Int,
    val auto_id: Int
)

data class CheckShareToken(
    val cid: Int,
    val bid: Int,
    val uid: Int
)

data class ShrBundle(

    @SerializedName("auto_id")
    val autoId: Int,

    @SerializedName("bid")
    val bid: Int,

    @SerializedName("bundle_code")
    val bundleCode: String,

    @SerializedName("bundle_date")
    val bundleDate: String,

    @SerializedName("bundle_expiry")
    val bundleExpiry: String,

    @SerializedName("bundle_name")
    val bundleName: String,

    @SerializedName("bundle_rmk1")
    val bundleRmk1: String,

    @SerializedName("bundle_rmk2")
    val bundleRmk2: String,

    @SerializedName("cid")
    val cid: Int,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("customer_id")
    val customerId: Int,

    @SerializedName("status")
    val status: Int,

    @SerializedName("user_id")
    val userId: Int
)

data class ShrCustomer(
    @SerializedName("auto_id")
    val autoId: Int,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("customer_code")
    val customerCode: String,

    @SerializedName("customer_contact")
    val customerContact: String,

    @SerializedName("customer_email")
    val customerEmail: String,

    @SerializedName("customer_name")
    val customerName: String,

    @SerializedName("customer_phone")
    val customerPhone: String,

    @SerializedName("customer_remark")
    val customerRemark: String,

    @SerializedName("status")
    val status: Int,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("vergi_dairesi")
    val vergiDairesi: String,

    @SerializedName("vergi_no")
    val vergiNo: String

)