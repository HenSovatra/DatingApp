// models/ApiErrorResponse.kt (or add to UserApiModels.kt)
package com.example.datingapp.models

import com.google.gson.annotations.SerializedName

data class ApiErrorResponse(
    @SerializedName("non_field_errors") val nonFieldErrors: List<String>?,
    @SerializedName("email") val emailErrors: List<String>?,
    @SerializedName("password") val passwordErrors: List<String>?,
    @SerializedName("detail") val detail: String?   ,
    @SerializedName("username") val usernameErrors: List<String>?,
)