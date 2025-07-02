package com.example.datingapp.models

data class UserResponse(
    val id: Int,
    val username: String,
    val email: String,
    val profile: ProfileDataResponse?
)