package com.example.datingapp.fragments

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.datingapp.R
import com.example.datingapp.api.RetrofitClient
import com.example.datingapp.models.UserProfile // Used for sending profile updates
import com.example.datingapp.models.UserResponseSetting // Used for receiving profile data
import com.yourpackage.yourapp.auth.SessionManager // *** IMPORTANT: Ensure this path is correct for your SessionManager ***
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
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

    private lateinit var profileImageView: ImageView
    private lateinit var sessionManager: SessionManager
    private var userId: Int = -1

    private var selectedImageUri: Uri? = null

    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize the ActivityResultLauncher
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                imageUri?.let {
                    selectedImageUri = it
                    Glide.with(this)
                        .load(it)
                        .placeholder(R.drawable.defaultpfp)
                        .error(R.drawable.defaultpfp)
                        .into(editProfileImage)
                } ?: Toast.makeText(requireContext(), "Failed to get image URI.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Image selection cancelled.", Toast.LENGTH_SHORT).show()
            }
        }
    }


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
            showSaveConfirmationDialog()
        }

        etDateOfBirth.setOnClickListener {
            showDatePickerDialog()
        }

        btnChangeProfileImage.setOnClickListener {
            openImagePicker()
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
                        val userResponseSetting = response.body()!!
                        populateFields(userResponseSetting)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(
                            TAG,
                            "Failed to load profile for editing: ${response.code()} - $errorBody"
                        )
                        Toast.makeText(
                            requireContext(),
                            "Failed to load profile data: ${errorBody ?: "Unknown error"}",
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

    private fun populateFields(userResponse: UserResponseSetting) {
        val profile = userResponse.profileData
        profile?.let {
            etFirstName.setText(it.firstName)
            etLastName.setText(it.lastName)
            etDateOfBirth.setText(it.dateOfBirth)

            val genderOptions = resources.getStringArray(R.array.gender_options)
            val currentGender = it.gender?.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString() }
            val selectedIndex = genderOptions.indexOfFirst { option -> option == currentGender }
            if (selectedIndex != -1) {
                spinnerGender.setSelection(selectedIndex)
            }

            etPreferredDateType.setText(it.kindOfDateLookingFor?.name)
            etBio.setText(it.bio)
            etLocation.setText(it.location)

            it.profileImageUrl?.let { imageUrl ->
                if (imageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.defaultpfp)
                        .error(R.drawable.defaultpfp)
                        .into(editProfileImage)
                } else {
                    editProfileImage.setImageResource(R.drawable.defaultpfp)
                }
            } ?: run {
                editProfileImage.setImageResource(R.drawable.defaultpfp)
            }
        } ?: run {
            Log.e(TAG, "Profile data is null in UserResponseSetting.")
            Toast.makeText(requireContext(), "Profile data not found in response.", Toast.LENGTH_SHORT).show()
        }
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

    private fun showSaveConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Save")
            .setMessage("Are you sure you want to save these changes to your profile?")
            .setPositiveButton("Save") { dialog, _ ->
                dialog.dismiss()
                saveUserProfile()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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

        lifecycleScope.launch(Dispatchers.IO) {
            selectedImageUri?.let { uri ->
                try {
                    uploadImageToServer(uri)
                    selectedImageUri = null
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Failed to upload image. Profile not saved.", Toast.LENGTH_LONG).show()
                    }
                    Log.e(TAG, "Image upload failed before profile save: ${e.message}", e)
                    return@launch
                }
            }

            val updatedProfile = UserProfile(
                id = userId,
                username = null,
                email = null,
                firstName = firstName,
                lastName = lastName,
                dateOfBirth = dateOfBirth,
                gender = gender,
                kindOfDateLookingFor = null,
                bio = bio,
                location = location,
                profileImageUrl = null,
                preferredDateType = preferredDateType
            )

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
                            "Failed to save profile: ${errorBody ?: "Unknown error"}",
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
                        "Server error saving profile: ${e.message()}",
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
    private fun openImagePicker() {
        val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = "image/*"
            }
        } else {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
        }
        pickImageLauncher.launch(intent)
    }

    private suspend fun uploadImageToServer(imageUri: Uri) {
        val authToken = sessionManager.getAuthToken()
        if (authToken.isNullOrEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Authentication required to upload image.", Toast.LENGTH_SHORT).show()
            }
            throw IllegalStateException("Authentication token is missing.")
        }

        var imageFile: File? = null
        try {
            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            val originalFileName = requireContext().contentResolver.getFileName(imageUri) ?: "profile_image.jpg"
            val tempFile = File(requireContext().cacheDir, originalFileName)
            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            imageFile = tempFile

            if (imageFile != null && imageFile.exists()) {
                val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("profile_image", imageFile.name, requestFile)

                val response = RetrofitClient.apiService.uploadProfileImage(userId, "Token $authToken", body)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val updatedProfileResponse = response.body()!!
                        updatedProfileResponse.profileData?.profileImageUrl?.let { newImageUrl ->
                            Glide.with(this@EditProfileFragment)
                                .load(newImageUrl)
                                .placeholder(R.drawable.defaultpfp)
                                .error(R.drawable.defaultpfp)
                                .into(editProfileImage)
                        }
                        Toast.makeText(requireContext(), "Profile picture uploaded!", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Profile image uploaded successfully.")
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Failed to upload image: ${response.code()} - ${errorBody}"
                        Log.e(TAG, errorMessage)
                        throw IOException(errorMessage)
                    }
                }
            } else {
                val errorMessage = "Could not create image file from URI."
                Log.e(TAG, errorMessage)
                throw IOException(errorMessage)
            }
        } finally {
            imageFile?.delete()
            Log.d(TAG, "Temporary image file cleaned up.")
        }
    }

    private fun android.content.ContentResolver.getFileName(uri: Uri): String? {
        var name: String? = null
        query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (columnIndex != -1) {
                    name = cursor.getString(columnIndex)
                }
            }
        }
        return name
    }
}