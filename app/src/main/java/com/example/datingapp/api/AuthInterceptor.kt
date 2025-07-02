package com.example.datingapp.api
import android.util.Log
import com.yourpackage.yourapp.auth.SessionManager
import okhttp3.Interceptor
import okhttp3.Response


class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val authToken = sessionManager.getAuthToken()

        val requestBuilder = originalRequest.newBuilder()
        authToken?.let {
            requestBuilder.header("Authorization", "Token $it")
            Log.d("AuthInterceptor:"," Adding Authorization header with token.")
        } ?: run {
            Log.d("AuthInterceptor:"," No auth token found.")
        }

        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}