package com.example.datingapp.fragments

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.datingapp.activities.HomeActivity
import com.example.datingapp.R
import com.example.datingapp.api.RetrofitClient
import com.example.datingapp.models.ApiErrorResponse
import com.example.datingapp.models.RegisterConstants
import com.example.datingapp.models.UserLoginRequest
import com.example.datingapp.utils.showAlertDialog
import com.google.gson.Gson
import com.yourpackage.yourapp.auth.SessionManager
import kotlinx.coroutines.launch

class authentication : Fragment() {

    private lateinit var loginCardView: CardView
    private lateinit var signUpTextView: TextView
    private lateinit var loginBtn : Button
    private lateinit var usernameEditText : EditText
    private lateinit var passwordEditText : EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_authentication, container, false)
        loginCardView = view.findViewById(R.id.login_card_view)
        signUpTextView = view.findViewById(R.id.signup)
        usernameEditText = view.findViewById(R.id.email_edit_text)
        passwordEditText = view.findViewById(R.id.password_edit_text)
        loginBtn = view.findViewById(R.id.login_button)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val popUpAnimation = AnimatorInflater.loadAnimator(context, R.animator.pop_up_animation) as AnimatorSet
        popUpAnimation.setTarget(loginCardView)
        popUpAnimation.start()

        loginBtn.setOnClickListener {
            val loginRequest = UserLoginRequest(
                username = usernameEditText.text.toString(),
                password = passwordEditText.text.toString()
            )

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.apiService.loginUser(loginRequest)

                    if (response.isSuccessful) {
                        val authResponse = response.body()
                        authResponse?.let {
                            saveAuthToken(it.token, it.user.id, it.user.email)
                            Toast.makeText(requireContext(), "Login Successful!", Toast.LENGTH_SHORT).show()
                            Log.d("LOGIN_SUCCESS", "Token: ${it.token}, User ID: ${it.user.id}, Email: ${it.user.email}")
                            val intent = Intent(
                                activity,
                                HomeActivity::class.java
                            )
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            activity?.finish()

                        } ?: run {
                            showAlertDialog("Login Issue", "Login successful but no response data received.")
                        }
                    } else {
                        var errorMessage: String = "Unknown error occurred."
                        val errorBody = response.errorBody()?.string()
                        Log.e("LOGIN_FAILED_API", "Error Body (Status ${response.code()}): $errorBody")

                        if (errorBody != null) {
                            try {
                                val apiErrorResponse = Gson().fromJson(errorBody, ApiErrorResponse::class.java)

                                val errors = mutableListOf<String>()
                                apiErrorResponse.nonFieldErrors?.forEach { errors.add(it) }
                                apiErrorResponse.usernameErrors?.forEach { errors.add("Username/Email: $it") }
                                apiErrorResponse.passwordErrors?.forEach { errors.add("Password: $it") }
                                apiErrorResponse.detail?.let { errors.add(it) }

                                if (errors.isNotEmpty()) {
                                    errorMessage = "Login Failed:\n" + errors.joinToString("\n")
                                } else {
                                    errorMessage = "Login failed with code: ${response.code()}. Server response: $errorBody"
                                }
                            } catch (e: Exception) {
                                Log.e("API_ERROR_PARSE", "Failed to parse error body: $errorBody", e)
                                errorMessage = "Login failed with code: ${response.code()}. Server response: $errorBody"
                            }
                        } else {
                            errorMessage = "Login failed: ${response.code()} ${response.message() ?: "No message"}"
                        }
                        showAlertDialog("Login Failed", errorMessage)
                    }
                } catch (e: Exception) {
                    Log.e("NETWORK_ERROR", "Login failed: ${e.message}", e)
                    showAlertDialog("Network Error", "Unable to connect to the server. Please check your internet connection and try again.")
                }
            }
        }

        signUpTextView.setOnClickListener {
            val signUpFragment = signup_fragment()
            val sharedPrefs = requireContext().getSharedPreferences(
                RegisterConstants.PREFS_NAME,
                Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                putString(RegisterConstants.KEY_USERNAME, "")
                putString(RegisterConstants.KEY_EMAIL, "")
                putString(RegisterConstants.KEY_PASSWORD, "")
                putString(RegisterConstants.KEY_DATE_OF_BIRTH, "")
                putString(RegisterConstants.KEY_FIRST_NAME, "")
                putString(RegisterConstants.KEY_LAST_NAME, "")
                putString(RegisterConstants.KEY_GENDER, "")
                putString(RegisterConstants.KEY_DATE_OF_BIRTH_DISPLAY,"Day    |   Month   |    Year")
                putString(RegisterConstants.KEY_KIND_OF_DATE_LOOKING_FOR, "")
                apply()
            }

            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, signUpFragment)
                .addToBackStack(null)
                .commit()
        }

    }
    private fun saveAuthToken(token: String, userId: Int, email: String) {
        val sessionManager = SessionManager(requireContext())
        sessionManager.createLoginSession(token, userId, email)
        Log.d("Auth", "Token saved: $token, ID: $userId, Email: $email")
    }

}