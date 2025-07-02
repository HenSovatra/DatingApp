package com.example.datingapp.models

import androidx.annotation.DrawableRes

data class KindOfDate(
    val id: String,
    val name: String,
    @DrawableRes val iconResId: Int,
    var isActive: Boolean = false
)
