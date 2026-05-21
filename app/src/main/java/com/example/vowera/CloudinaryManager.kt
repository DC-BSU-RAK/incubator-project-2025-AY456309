package com.example.vowera.utils

import android.content.Context
import com.cloudinary.android.MediaManager

object CloudinaryManager {

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return

        val config = hashMapOf(
            "cloud_name" to "dshlfuqkq"
        )

        MediaManager.init(context, config)
        isInitialized = true
    }
}