package com.example.datingapp.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout // Import FrameLayout for the loading overlay
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.datingapp.R
import com.example.datingapp.api.RetrofitClient // Make sure you have this import
import com.example.datingapp.models.ApiErrorResponse // Assuming you have this for error parsing
import com.example.datingapp.models.RegisterConstants
import com.google.gson.Gson // For parsing error bodies
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class signup_fragment : Fragment() {

    private lateinit var BtnBack: LinearLayout
    private lateinit var BtnNext: Button
    private lateinit var TxtEmail: EditText
    private lateinit var TxtPassword: EditText
    private lateinit var TxtConfirmPassword: EditText
    private lateinit var loadingOverlay: FrameLayout

    private var isProcessingClick = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signup_fragment, container, false)
        BtnBack = view.findViewById(R.id.backBtn)
        BtnNext = view.findViewById(R.id.next_button)
        TxtEmail = view.findViewById(R.id.email_edit_text)
        TxtPassword = view.findViewById(R.id.password_edit_text)
        TxtConfirmPassword = view.findViewById(R.id.confirm_password_edit_text)
        loadingOverlay = view.findViewById(R.id.loadingOverlay)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPrefs = requireContext().getSharedPreferences(RegisterConstants.PREFS_NAME, Context.MODE_PRIVATE)
        TxtEmail.setText(sharedPrefs.getString(RegisterConstants.KEY_EMAIL, ""))
        TxtPassword.setText(sharedPrefs.getString(RegisterConstants.KEY_PASSWORD, ""))
        TxtConfirmPassword.setText(sharedPrefs.getString(RegisterConstants.KEY_PASSWORD, ""))

        BtnBack.setOnClickListener {
            val loginFragment = authentication()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, loginFragment)
                .addToBackStack(null)
                .commit()
        }

        BtnNext.setOnClickListener {
            if (isProcessingClick) {
                Log.d("SignUpFragment", "Ignoring click because already processing.")
                return@setOnClickListener
            }

            val email = TxtEmail.text.toString().trim()
            val password = TxtPassword.text.toString().trim()
            val confirmPassword = TxtConfirmPassword.text.toString().trim()

            if (email.isEmpty()) {
                TxtEmail.error = "Email cannot be empty"
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                TxtEmail.error = "Please enter a valid email address"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                TxtPassword.error = "Password cannot be empty"
                return@setOnClickListener
            }
            if(password.length<8){
                TxtPassword.error = "Password must be more than 8 digits"
                return@setOnClickListener
            }
            if (confirmPassword.isEmpty()) {
                TxtConfirmPassword.error = "Confirm password cannot be empty"
                return@setOnClickListener
            }

            if (confirmPassword != password) {
                TxtConfirmPassword.error = "Password does not match"
                return@setOnClickListener
            }
            checkEmailExistence(email, password, confirmPassword)
        }
    }

    private fun checkEmailExistence(email: String, password: String, confirmPassword: String) {
        isProcessingClick = true
        BtnNext.isEnabled = false
        loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.checkEmailExists(email)

                if (response.isSuccessful) {
                    val sharedPrefs = requireContext().getSharedPreferences(
                        RegisterConstants.PREFS_NAME,
                        Context.MODE_PRIVATE
                    )
                    with(sharedPrefs.edit()) {
                        putString(RegisterConstants.KEY_USERNAME, email)
                        putString(RegisterConstants.KEY_EMAIL, email)
                        putString(RegisterConstants.KEY_PASSWORD, confirmPassword)
                        putString(RegisterConstants.KEY_PASSWORD_CONFIRM, confirmPassword)
                        apply()
                    }

                    val nextFragment = signupname_fragment()
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                        .replace(R.id.fragment_container, nextFragment)
                        .addToBackStack(null)
                        .commit()

                } else {
                    handleEmailCheckError(response.code(), response.errorBody()?.string())
                }
            } catch (e: HttpException) {
                handleEmailCheckError(e.code(), e.response()?.errorBody()?.string())
            } catch (e: IOException) {
                Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("SignUpFragment", "Network error during email check", e)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "An unexpected error occurred: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("SignUpFragment", "Unexpected error during email check", e)
            } finally {
                loadingOverlay.visibility = View.GONE
                BtnNext.isEnabled = true
                isProcessingClick = false
            }
        }
    }

    private fun handleEmailCheckError(statusCode: Int, errorBody: String?) {
        when (statusCode) {
            409 -> {
                TxtEmail.error = "This email is already registered."
                Toast.makeText(requireContext(), "This email is already registered.", Toast.LENGTH_LONG).show()
            }
            400 -> {
                val errorMessage = parseErrorMessage(errorBody, "Invalid email format or request.")
                TxtEmail.error = errorMessage
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
            else -> {
                val errorMessage = parseErrorMessage(errorBody, "Failed to check email availability. Please try again.")
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                Log.e("SignUpFragment", "Email check failed with code $statusCode: $errorBody")
            }
        }
    }

    private fun parseErrorMessage(errorBody: String?, defaultMessage: String): String {
        return if (errorBody != null) {
            try {
                val apiError = Gson().fromJson(errorBody, ApiErrorResponse::class.java)
                apiError.detail ?: apiError.nonFieldErrors?.firstOrNull() ?: defaultMessage
            } catch (e: Exception) {
                Log.e("SignUpFragment", "Failed to parse error body: $errorBody", e)
                defaultMessage
            }
        } else {
            defaultMessage
        }
    }
}