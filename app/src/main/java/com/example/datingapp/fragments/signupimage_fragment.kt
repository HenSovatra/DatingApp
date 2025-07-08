package com.example.datingapp.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout // Import FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import com.example.datingapp.activities.HomeActivity
import com.example.datingapp.models.ImageItem
import com.example.datingapp.R
import com.example.datingapp.adapters.ImageSlotAdapter
import com.example.datingapp.api.RetrofitClient
import com.example.datingapp.models.UserRegistrationRequest
import com.google.gson.Gson
import kotlinx.coroutines.launch
import com.example.datingapp.models.ApiErrorResponse
import com.example.datingapp.models.RegisterConstants
import com.example.datingapp.models.base64Data
import com.example.datingapp.utils.showAlertDialog
import com.yourpackage.yourapp.auth.SessionManager
import java.io.InputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job


class signupimage_fragment : Fragment() {

    private lateinit var backBtn: LinearLayout
    private lateinit var nextButton: Button
    private lateinit var imageSlotsRecyclerView: RecyclerView
    private lateinit var imageSlotAdapter: ImageSlotAdapter
    private lateinit var loadingOverlay: FrameLayout

    private var currentSelectedSlotPosition: Int = -1
    private var isProcessingClick = false


    private val pickImageLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null && currentSelectedSlotPosition != -1) {
                    imageSlotAdapter.updateImageSlot(currentSelectedSlotPosition, imageUri)
                    currentSelectedSlotPosition = -1
                }
            }
        }

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openImagePicker()
            } else {
                Toast.makeText(requireContext(), "Permission to access gallery denied.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signupimage_fragment, container, false)

        backBtn = view.findViewById(R.id.backBtn)
        nextButton = view.findViewById(R.id.next_button)
        imageSlotsRecyclerView = view.findViewById(R.id.imageSlotsRecyclerView)
        loadingOverlay = view.findViewById(R.id.loadingOverlay) // NEW: Initialize the overlay

        val imageItems = mutableListOf<ImageItem>()
        for (i in 1..6) {
            imageItems.add(ImageItem(id = "slot_$i", imageUri = null, isAddButton = true))
        }

        imageSlotsRecyclerView.layoutManager = GridLayoutManager(context, 3)
        imageSlotAdapter = ImageSlotAdapter(
            imageItems,
            onAddImageClick = { position ->
                currentSelectedSlotPosition = position
                checkAndRequestPermission()
            },
            onDeleteImageClick = { position ->
                imageSlotAdapter.updateImageSlot(position, null)
                Toast.makeText(
                    requireContext(),
                    "Image cleared from slot ${position + 1}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        imageSlotsRecyclerView.adapter = imageSlotAdapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backBtn.setOnClickListener {
            val prevFragment = signuptype_fragment()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, prevFragment)
                .addToBackStack(null)
                .commit()
        }

        nextButton.setOnClickListener {
            if (isProcessingClick) {
                Log.d(TAG, "Ignoring click because already processing.")
                return@setOnClickListener
            }

            val selectedUris = imageSlotAdapter.getAllImageUris()
            if (selectedUris.isEmpty()) {
                Toast.makeText(requireContext(), "Please add at least one photo.", Toast.LENGTH_SHORT).show()
            } else {
                isProcessingClick = true
                nextButton.isEnabled = false
                loadingOverlay.visibility = View.VISIBLE // NEW: Show the loading overlay

                val sharedPrefs = requireContext().getSharedPreferences(RegisterConstants.PREFS_NAME, Context.MODE_PRIVATE);
                val username = sharedPrefs.getString(RegisterConstants.KEY_USERNAME, "")
                val password= sharedPrefs.getString(RegisterConstants.KEY_PASSWORD, "")
                val firstname= sharedPrefs.getString(RegisterConstants.KEY_FIRST_NAME, "")
                val lastname= sharedPrefs.getString(RegisterConstants.KEY_LAST_NAME, "")
                val dateOfBirth= sharedPrefs.getString(RegisterConstants.KEY_DATE_OF_BIRTH, "")
                val gender= sharedPrefs.getString(RegisterConstants.KEY_GENDER, "")
                val selectedInterestId= sharedPrefs.getString(RegisterConstants.KEY_KIND_OF_DATE_LOOKING_FOR, "")

                val userRequest = UserRegistrationRequest(
                    username = username.toString(),
                    email = username.toString(),
                    password = password.toString(),
                    passwordConfirm = password.toString(),
                    firstName = firstname.toString(),
                    lastName = lastname.toString(),
                    dateOfBirth = dateOfBirth.toString(),
                    gender = gender.toString(),
                    kindOfDateLookingFor = selectedInterestId!!.toInt(),
                    bio = "",
                    location = ""
                )

                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.apiService.registerUser(userRequest)

                        if (response.isSuccessful) {
                            val registrationBody = response.body()
                            if (registrationBody?.user?.email != null && registrationBody.user.id != null && registrationBody.token != null) {
                                val sessionManager = SessionManager(requireContext())
                                sessionManager.createLoginSession(registrationBody.token, registrationBody.user.id, registrationBody.user.email)
                                val data : MutableList<base64Data>  = mutableListOf();

                                for (uri in selectedUris) {
                                    val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
                                    inputStream?.use { input ->
                                        val bytes = input.readBytes()
                                        val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                        val fileName = getFileNameFromUri(uri, requireContext())
                                        data.add(base64Data(
                                            fileName = fileName,
                                            base64Data = base64String
                                        ));
                                    }
                                }
                                val responseImage = RetrofitClient.userService.saveImages(registrationBody.user.id,data)
                                if(responseImage.isSuccessful){
                                    Log.d("image save","successss")
                                }

                                // All good, navigate away
                                val intent = Intent(requireContext(), HomeActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                requireActivity().finish()

                            } else {
                                Log.e(TAG, "Registration successful but missing user data in response.")
                                showAlertDialog("Registration Error", "Successful but missing user data in response. Please try again.")
                            }

                        } else {
                            val errorBody = response.errorBody()?.string()
                            var  errorMessage: String
                            if (errorBody != null) {
                                try {
                                    val apiErrorResponse = Gson().fromJson(errorBody, ApiErrorResponse::class.java)
                                    val errors = mutableListOf<String>()
                                    apiErrorResponse.nonFieldErrors?.forEach { errors.add(it) }
                                    apiErrorResponse.emailErrors?.forEach { errors.add("Email: $it") }
                                    apiErrorResponse.passwordErrors?.forEach { errors.add("Password: $it") }
                                    apiErrorResponse.detail?.let { errors.add(it) }

                                    errorMessage = if (errors.isNotEmpty()) {
                                        errors.joinToString("\n")
                                    } else {
                                        "Registration failed with code: ${response.code()}"
                                    }

                                } catch (e: Exception) {
                                    Log.e("API_ERROR", "Failed to parse error body: $errorBody", e)
                                    errorMessage = "Registration failed with code: ${response.code()}. Server response: $errorBody"
                                }
                            } else {
                                errorMessage = "Registration failed: ${response.code()} ${response.message()}"
                            }
                            showAlertDialog("Registration Failed", errorMessage)
                        }
                    } catch (e: Exception) {
                        Log.e("API_NETWORK_ERROR", "Error during registration API call", e)
                        showAlertDialog("Network Error", "Could not connect to server. Please check your internet connection or try again later. Error: ${e.message}")
                    } finally {
                        // NEW: Hide the loading overlay
                        loadingOverlay.visibility = View.GONE
                        nextButton.isEnabled = true
                        isProcessingClick = false
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(requireContext(), "Photos permission is needed to select images.", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(permission)
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    fun getFileNameFromUri(uri: Uri, context: Context): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "image_${System.currentTimeMillis()}.jpg"
    }
}