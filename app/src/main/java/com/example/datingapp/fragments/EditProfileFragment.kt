package com.example.datingapp.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.datingapp.R
import com.example.datingapp.api.RetrofitClient
import com.example.datingapp.models.UserProfile
import com.example.datingapp.models.UserResponseSetting
import com.yourpackage.yourapp.auth.SessionManager
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditProfileFragment : Fragment() {

    private val TAG = "EditProfileFragment"

    private lateinit var backButton: ImageButton
    private lateinit var btnSave: Button
    private lateinit var editProfileImage: CircleImageView
    private lateinit var btnChangeProfileImage: Button
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etDateOfBirth: EditText
    private lateinit var spinnerGender: Spinner
    private lateinit var etPreferredDateType: EditText
    private lateinit var etBio: EditText
    private lateinit var etLocation: EditText

    private lateinit var sessionManager: SessionManager
    private var userId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)

        backButton = view.findViewById(R.id.backButton)
        btnSave = view.findViewById(R.id.btnSave)
        editProfileImage = view.findViewById(R.id.editProfileImage)
        btnChangeProfileImage = view.findViewById(R.id.btnChangeProfileImage)
        etFirstName = view.findViewById(R.id.etFirstName)
        etLastName = view.findViewById(R.id.etLastName)
        etDateOfBirth = view.findViewById(R.id.etDateOfBirth)
        spinnerGender = view.findViewById(R.id.spinnerGender)
        etPreferredDateType = view.findViewById(R.id.etPreferredDateType)
        etBio = view.findViewById(R.id.etBio)
        etLocation = view.findViewById(R.id.etLocation)

        sessionManager = SessionManager(requireContext())
        userId = sessionManager.getUserId()

        setupGenderSpinner()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (userId != -1) {
            loadUserProfile(userId)
        } else {
            Toast.makeText(requireContext(), "User ID not found.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSave.setOnClickListener {
            saveUserProfile()
        }

        etDateOfBirth.setOnClickListener {
            showDatePickerDialog()
        }

        btnChangeProfileImage.setOnClickListener {
            Toast.makeText(requireContext(), "Image change coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupGenderSpinner() {
        val genderOptions = resources.getStringArray(R.array.gender_options)
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, genderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = adapter
    }

    private fun loadUserProfile(userId: Int) {
        val authToken = sessionManager.getAuthToken()
        if (authToken.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Authentication required to load profile.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getUserProfile(userId, "Token $authToken")
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val userProfile = response.body()!!
                        Log.d("userprofile",userProfile.toString())
                        populateFields(userProfile)
                        Log.d(TAG, "Profile loaded successfully for editing.")
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(
                            TAG,
                            "Failed to load profile for editing: ${response.code()} - $errorBody"
                        )
                        Toast.makeText(
                            requireContext(),
                            "Failed to load profile data.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Network error loading profile: ${e.message}", e)
                    Toast.makeText(
                        requireContext(),
                        "Network error. Check internet connection.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "HTTP error loading profile: ${e.code()}", e)
                    Toast.makeText(
                        requireContext(),
                        "Server error loading profile: ${e.message()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Unexpected error loading profile: ${e.message}", e)
                    Toast.makeText(
                        requireContext(),
                        "Error loading profile data.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun populateFields(profile: UserResponseSetting) {
        etFirstName.setText(profile.profileData?.firstName)
        etLastName.setText(profile.profileData?.lastName)
        etDateOfBirth.setText(profile.profileData?.dateOfBirth)

        val genderOptions = resources.getStringArray(R.array.gender_options)
        val currentGender = profile.profileData?.gender?.capitalize(Locale.ROOT)
        val selectedIndex = genderOptions.indexOfFirst { it == currentGender }
        if (selectedIndex != -1) {
            spinnerGender.setSelection(selectedIndex)
        }

        etPreferredDateType.setText(profile.profileData?.kindOfDateLookingFor?.name)
        etBio.setText(profile.profileData?.bio)
        etLocation.setText(profile.profileData?.location)

        Glide.with(this)
            .load(profile.profileData?.profileImageUrl)
            .placeholder(R.drawable.defaultpfp)
            .error(R.drawable.defaultpfp)
            .into(editProfileImage)
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        if (etDateOfBirth.text.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(etDateOfBirth.text.toString())
                if (date != null) {
                    calendar.time = date
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing DOB for date picker: ${e.message}")
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDayOfMonth)
                }
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                etDateOfBirth.setText(dateFormat.format(selectedDate.time))
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun saveUserProfile() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val dateOfBirth = etDateOfBirth.text.toString().trim()
        val gender = spinnerGender.selectedItem.toString()
        val preferredDateType = etPreferredDateType.text.toString().trim()
        val bio = etBio.text.toString().trim()
        val location = etLocation.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty() || dateOfBirth.isEmpty() || gender.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val authToken = sessionManager.getAuthToken()
        if (authToken.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Authentication required to save profile.", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedProfile = UserProfile(
            id = -1,
            username = null,
            email = null,
            firstName = firstName,
            lastName = lastName,
            dateOfBirth = dateOfBirth,
            gender = gender,
            preferredDateType = preferredDateType,
            bio = bio,
            location = location,
            profileImageUrl = null,
            kindOfDateLookingFor = null
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.updateUserProfile(userId, "Token $authToken", updatedProfile)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            requireContext(),
                            "Profile updated successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        parentFragmentManager.popBackStack()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Failed to save profile: ${response.code()} - $errorBody")
                        Toast.makeText(
                            requireContext(),
                            "Failed to save profile: $errorBody",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Network error saving profile: ${e.message}", e)
                    Toast.makeText(
                        requireContext(),
                        "Network error. Please check your internet connection.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Server error saving profile: ${e.code()} - ${e.message()}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(
                        requireContext(),
                        "Server error saving profile.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Unexpected error saving profile: ${e.message}", e)
                    Toast.makeText(requireContext(), "Error saving profile.", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }
}