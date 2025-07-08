// SplashActivity.kt
package com.example.datingapp.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.datingapp.R
import com.example.datingapp.api.RetrofitClient

class SplashActivity : AppCompatActivity() {

    private val SPLASH_TIME_OUT: Long = 3000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        RetrofitClient.init(this)
        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginStatusAndNavigate()
        }, SPLASH_TIME_OUT)
    }

    private fun checkLoginStatusAndNavigate() {
        val sharedPref = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val authToken = sharedPref.getString("auth_token", null)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

        val nextActivityIntent: Intent
        if (authToken != null && isLoggedIn) {
            Log.d("AuthCheck", "User is already logged in. Redirecting to HomeActivity.")
            nextActivityIntent = Intent(this, HomeActivity::class.java)
        } else {
            Log.d("AuthCheck", "User is not logged in. Redirecting to MainActivity (authentication flow).")
            nextActivityIntent = Intent(this, MainActivity::class.java)
        }

        nextActivityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(nextActivityIntent)
        finish()
    }
}