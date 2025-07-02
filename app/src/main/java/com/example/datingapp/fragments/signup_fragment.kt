package com.example.datingapp.fragments

import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.datingapp.R
import com.example.datingapp.models.RegisterConstants

class signup_fragment : Fragment() {

    private lateinit var BtnBack: LinearLayout
    private  lateinit var BtnNext: Button;
    private lateinit var TxtEmail: EditText;
    private  lateinit var TxtPassword : EditText;
    private lateinit var TxtConfirmPassword : EditText;

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signup_fragment, container, false)
        BtnBack = view.findViewById(R.id.backBtn);
        BtnNext = view.findViewById(R.id.next_button)
        TxtEmail = view.findViewById(R.id.email_edit_text)
        TxtPassword = view.findViewById(R.id.password_edit_text);
        TxtConfirmPassword = view.findViewById(R.id.confirm_password_edit_text);
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

        BtnNext.setOnClickListener nextButtonClickListener@{
            val email = TxtEmail.text.toString().trim()
            val password = TxtPassword.text.toString().trim()
            val confirmPassword = TxtConfirmPassword.text.toString().trim()

            if (email.isEmpty()) {
                TxtEmail.error = "Email cannot be empty"
                return@nextButtonClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                TxtEmail.error = "Please enter a valid email address"
                return@nextButtonClickListener
            }

            if (password.isEmpty()) {
                TxtPassword.error = "Password cannot be empty"
                return@nextButtonClickListener
            }

            if (confirmPassword.isEmpty()) {
                TxtConfirmPassword.error = "Confirm password cannot be empty"
                return@nextButtonClickListener
            }

            if (confirmPassword != password) {
                TxtConfirmPassword.error = "Password does not match"
                return@nextButtonClickListener
            }

            val sharedPrefs = requireContext().getSharedPreferences(
                RegisterConstants.PREFS_NAME,
                Context.MODE_PRIVATE
            )
            with(sharedPrefs.edit()) {
                putString(RegisterConstants.KEY_USERNAME, email) // Assuming username is email
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
        }
    }
}