

package com.yourpackage.yourapp.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.datingapp.api.RetrofitClient
import com.example.datingapp.models.FcmTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SessionManager(private val context: Context) {

    private val PREF_NAME = "auth_prefs"
    private val KEY_AUTH_TOKEN = "auth_token"
    private val KEY_USER_ID = "user_id"
    private val KEY_USER_EMAIL = "user_email"
    private val KEY_IS_LOGGED_IN = "is_logged_in"

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    fun createLoginSession(token: String, userId: Int, email: String) {
        editor.putString(KEY_AUTH_TOKEN, token)
        editor.putInt(KEY_USER_ID, userId)
        editor.putString(KEY_USER_EMAIL, email)
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.apply() // Use apply() for asynchronous saving
        sendFcmTokenToBackend()
        Log.d("SessionManager", "Login session created - Token saved: $token, ID: $userId, Email: $email")
    }

    fun sendFcmTokenToBackend() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_Token", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("FCM_Token", "Current FCM Token: $token")

            token?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val requestBody = FcmTokenRequest(token = it)
                        val response = RetrofitClient.apiService.registerFCMToken(requestBody)
                        if (response.isSuccessful) {
                            Log.d("FCM_Token", "FCM token sent to backend successfully.")
                        } else {
                            Log.e("FCM_Token", "Failed to send FCM token to backend: ${response.code()} - ${response.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e("FCM_Token", "Error sending FCM token to backend: ${e.message}", e)
                    }
                }
            }
        }
    }
    fun getAuthToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
    }

    fun getUserId(): Int {
        return sharedPreferences.getInt(KEY_USER_ID, -1)
    }

    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logoutUser() {
        editor.clear()
        editor.apply()

        Log.d("SessionManager", "User logged out. Session data cleared.")
    }
}