package com.example.datingapp.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar // Import ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.datingapp.activities.MainActivity
import com.example.datingapp.R
import com.example.datingapp.api.RetrofitClient
import com.yourpackage.yourapp.auth.SessionManager
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.util.Date
import java.util.Locale

class setting_fragment : Fragment() {

    private val TAG = "SettingFragment"

    private lateinit var authPrefs: SharedPreferences
    private lateinit var txtUsername: TextView
    private lateinit var logoutButton: Button
    private lateinit var txtAge: TextView
    private lateinit var txtDob: TextView
    private lateinit var valueSex: TextView
    private lateinit var txtKindOfDate: TextView
    private lateinit var profileImage: CircleImageView
    private lateinit var editIcon: ImageView

    private lateinit var valueLikesReceived: TextView
    private lateinit var valueMatches: TextView

    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var loadingTextView: TextView
    private lateinit var contentLayout: View
    private lateinit var likeDetail : ConstraintLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_setting_fragment, container, false)

        txtUsername = view.findViewById(R.id.profileName)
        logoutButton = view.findViewById(R.id.logoutButton)
        txtAge = view.findViewById(R.id.valueAge)
        txtDob = view.findViewById(R.id.valueDob)
        valueSex = view.findViewById(R.id.valueSex)
        txtKindOfDate = view.findViewById(R.id.valueDateType)
        profileImage = view.findViewById(R.id.imageViewProfile)
        editIcon = view.findViewById(R.id.editIcon)

        // NEW: Initialize loading UI elements and content layout
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        loadingTextView = view.findViewById(R.id.loadingTextView)
        contentLayout = view.findViewById(R.id.contentLayout) // Initialize contentLayout
        likeDetail = view.findViewById(R.id.likeDetail)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authPrefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

        val userId = authPrefs.getInt("user_id", -1)

        if (userId != -1) {
            Log.d(TAG, "Retrieved userId from SharedPreferences: $userId")
            setLoadingState(true, getString(R.string.loading_profile_data)) // Show loading initially
            fetchUserProfile(userId)
        } else {
            Log.e(TAG, "User ID not found in SharedPreferences. Cannot fetch profile.")
            Toast.makeText(requireContext(), "User not logged in or ID missing.", Toast.LENGTH_LONG).show()
            val intent = Intent(requireActivity(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
        likeDetail.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
                .replace(R.id.fragment_container, UserInteractionsContainerFragment())
                .addToBackStack(null) // Allows the user to press the back button to return here
                .commit()
        }
        val authToken = authPrefs.getString("auth_token", null)
        val email = authPrefs.getString("user_email", null)
        if (authToken != null) {
            Log.d(TAG, "Auth Token in SettingFragment: $authToken")
            Log.d(TAG, "Auth email in SettingFragment: $email")
        } else {
            Log.w(TAG, "Auth Token not found in SettingFragment.")
        }

        logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        editIcon.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, EditProfileFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setLoadingState(showLoading: Boolean, message: String? = null) {
        if (showLoading) {
            contentLayout.visibility = View.GONE
            loadingProgressBar.visibility = View.VISIBLE
            loadingTextView.visibility = View.VISIBLE
            loadingTextView.text = message ?: getString(R.string.loading_profile_data)
        } else {
            contentLayout.visibility = View.VISIBLE
            loadingProgressBar.visibility = View.GONE
            loadingTextView.visibility = View.GONE
        }
    }

    private fun fetchUserProfile(userId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sessionManager = SessionManager(requireContext())
                val authToken = sessionManager.getAuthToken()

                if (authToken.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Authentication required for profile.", Toast.LENGTH_SHORT).show()
                        setLoadingState(false)
                    }
                    return@launch
                }
                val response = RetrofitClient.userService.getUserById(userId, "Token $authToken")
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val user = response.body()
                        val profile = user?.profile
                        if (profile != null) {
                            val firstName = profile.firstName
                            val lastName = profile.lastName
                            val dateOfBirth = profile.dateOfBirth
                            val gender = profile.gender
                            val kindOfDateName = profile.kindOfDateLookingFor?.name
                            val imageUrl = profile.profileImageUrl

                            val displayFullName = "${firstName.orEmpty()} ${lastName.orEmpty()}".trim()
                            txtUsername.text = if (displayFullName.isNotEmpty()) displayFullName else user?.username ?: "Unknown User"

                            if (imageUrl.isNullOrEmpty()) {
                                Glide.with(profileImage.context)
                                    .load(R.drawable.defaultpfp)
                                    .into(profileImage)
                            } else {
                                Glide.with(profileImage.context)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.defaultpfp)
                                    .error(R.drawable.defaultpfp)
                                    .into(profileImage)
                            }

                            dateOfBirth?.let { dobString ->
                                try {
                                    txtAge.text = "${calculateAge(dobString)} years old"
                                    txtDob.text = formatDatabaseDateForDisplay(dobString)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error processing DOB: ${e.message}", e)
                                    txtAge.text = "N/A"
                                    txtDob.text = "N/A"
                                }
                            } ?: run {
                                txtAge.text = "N/A"
                                txtDob.text = "N/A"
                            }

                            when (gender?.lowercase(Locale.ROOT)) {
                                "male" -> {
                                    valueSex.text = "♂"
                                    valueSex.setTextColor(ContextCompat.getColor(requireContext(), R.color.male_symbol_blue))
                                }
                                "female" -> {
                                    valueSex.text = "♀"
                                    valueSex.setTextColor(ContextCompat.getColor(requireContext(), R.color.female_symbol_pink))
                                }
                                else -> {
                                    valueSex.text = gender ?: "N/A"
                                    valueSex.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color_light_gray))
                                }
                            }
                            txtKindOfDate.text = kindOfDateName.orEmpty()
                            Log.d(TAG, "User profile displayed successfully for $displayFullName")
                        } else {
                            Log.e(TAG, "User profile data is null in response.")
                            Toast.makeText(requireContext(), "Profile data incomplete.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Failed to fetch user profile: ${response.code()}" +
                                (errorBody?.let { " - $it" } ?: "")
                        Log.e(TAG, errorMessage)
                        Toast.makeText(requireContext(), "Failed to load profile data: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Network error: ${e.message}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(requireContext(), "Network error: Please check your internet connection.", Toast.LENGTH_LONG).show()
                }
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Server error: ${e.code()} - ${e.message()}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(requireContext(), "Server error fetching profile: ${e.message()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "An unexpected error occurred fetching profile: ${e.message}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(requireContext(), "Error fetching profile: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    setLoadingState(false)
                }
            }
        }
    }


    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { dialog, which ->
                performLogout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun performLogout() {
        val sessionManager = SessionManager(requireContext())
        sessionManager.logoutUser()
        val intent = Intent(requireActivity(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun calculateAge(birthDateString: String): Int {
        return try {
            val birthDate = LocalDate.parse(birthDateString)
            val currentDate = LocalDate.now()
            Period.between(birthDate, currentDate).years
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating age for DOB: $birthDateString", e)
            -1
        }
    }

    private fun formatDatabaseDateForDisplay(inputDateString: String): String {
        return try {
            val databaseInputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayOutputFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())

            val parsedDate: Date? = databaseInputFormat.parse(inputDateString)
            if (parsedDate != null) {
                displayOutputFormat.format(parsedDate)
            } else {
                Log.e(TAG, "Parsed date was null for input: $inputDateString")
                "N/A"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date for display: $inputDateString", e)
            "N/A"
        }
    }
}