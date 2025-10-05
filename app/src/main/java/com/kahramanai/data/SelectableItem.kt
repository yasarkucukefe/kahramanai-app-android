package com.kahramanai.data

import com.google.gson.annotations.SerializedName

data class SelectableItem(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
) {
    // We override toString() so that if you use this class in an ArrayAdapter (for a Spinner),
    // it will display the name correctly by default.
    override fun toString(): String {
        return name
    }
}