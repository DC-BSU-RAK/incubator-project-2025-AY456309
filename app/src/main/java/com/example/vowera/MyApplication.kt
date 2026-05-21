package com.example.vowera

import android.app.Application
import com.cloudinary.android.MediaManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = mapOf(
            "cloud_name" to "dshlfuqkq"
        )

        try {
            MediaManager.init(this, config)
        } catch (_: IllegalStateException) {
        }
    }
}