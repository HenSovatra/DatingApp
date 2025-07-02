// Create this file in your root package, e.g., com.example.datingapp/MyApplication.kt
package com.example.datingapp

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("MyApplication", "Application onCreate called. Initializing Firebase...")
        // Initialize Firebase here
        FirebaseApp.initializeApp(this)

        // Optional: Initialize other singletons or global components here
        // e.g., SessionManager.init(applicationContext) if it needs context
        // RetrofitClient.init(applicationContext) if it needs context
    }
}