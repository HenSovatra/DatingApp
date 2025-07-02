package com.example.datingapp.models

import android.net.Uri

data class ImageItem(
    val id: String,
    var imageUri: Uri? = null,
    var isAddButton: Boolean = true
)