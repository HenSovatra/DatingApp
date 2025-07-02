package com.example.datingapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.datingapp.R
import com.example.datingapp.api.RetrofitClient
import com.example.datingapp.models.InteractionRequest
import com.example.datingapp.models.ProfileDataResponse
import com.example.datingapp.models.UserProfileResponse
import com.yourpackage.yourapp.auth.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.CancellationException

class home_fragment : Fragment() {

    private var suggestedUsersList: MutableList<UserProfileResponse> = mutableListOf()
    private var currentUserIndex = 0
    private var currentPage = 1
    private var isLoading = false

    private lateinit var profileImageView: ImageView
    private lateinit var nameAgeTextView: TextView
    private lateinit var btnLike: TextView
    private lateinit var btnDislike: TextView
    private lateinit var btnSkip: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var loadingTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_fragment, container, false)
        profileImageView = view.findViewById(R.id.profileImageView)
        nameAgeTextView = view.findViewById(R.id.nameAgeTextView)
        btnLike = view.findViewById(R.id.btnLike)
        btnDislike = view.findViewById(R.id.btnDislike)
        btnSkip = view.findViewById(R.id.btnSkip)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        loadingTextView = view.findViewById(R.id.loadingTextView)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnLike.setOnClickListener {
            handleInteraction("like")
        }

        btnDislike.setOnClickListener {
            handleInteraction("dislike")
        }

        btnSkip.setOnClickListener {
            handleInteraction("skip")
        }

        setLoadingState(true, "Loading users...")
        fetchSuggestedUsers(currentPage)
    }

    private fun setLoadingState(showLoading: Boolean, message: String? = null) {
        if (showLoading) {
            // Show loading views
            loadingProgressBar.visibility = View.VISIBLE
            loadingTextView.visibility = View.VISIBLE
            loadingTextView.text = message ?: "Loading..."
            profileImageView.visibility = View.GONE
            nameAgeTextView.visibility = View.GONE
            btnLike.visibility = View.GONE
            btnDislike.visibility = View.GONE
            btnSkip.visibility = View.GONE
        } else {
            loadingProgressBar.visibility = View.GONE
            loadingTextView.visibility = View.GONE
            if (suggestedUsersList.isNotEmpty() && currentUserIndex < suggestedUsersList.size) {
                profileImageView.visibility = View.VISIBLE
                nameAgeTextView.visibility = View.VISIBLE
                btnLike.visibility = View.VISIBLE
                btnDislike.visibility = View.VISIBLE
                btnSkip.visibility = View.VISIBLE
            } else {
                clearUserDisplay("No more users. Please try again later!")
            }
        }
        setButtonsEnabled(!showLoading)
    }


    private fun fetchSuggestedUsers(page: Int = 1) {
        if (isLoading) return

        isLoading = true
        setLoadingState(true, "Fetching new users...")

        val sessionManager = SessionManager(requireContext())
        val authToken = sessionManager.getAuthToken()

        if (authToken.isNullOrEmpty()) {
            isLoading = false
            setLoadingState(false)
            Toast.makeText(requireContext(), "Authentication token missing. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getSuggestedUsers( page = page, pageSize = 10)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val suggestedUsersResponse = response.body()
                        val users: List<UserProfileResponse> = suggestedUsersResponse!!.results

                        if (users.isNotEmpty()) {
                            val isFirstLoad = suggestedUsersList.isEmpty()
                            suggestedUsersList.addAll(users)
                            Log.d("SuggestedUsers", "Fetched ${users.size} users. Total: ${suggestedUsersList.size}")

                            if (isFirstLoad || currentUserIndex >= suggestedUsersList.size - users.size) {
                                if (isFirstLoad) {
                                    currentUserIndex = 0
                                }
                                displayCurrentUser()
                            }
                            currentPage++
                        } else {
                            Log.d("SuggestedUsers", "No more users found on page $page.")
                            if (suggestedUsersList.isEmpty()) {
                                clearUserDisplay("No users found at the moment.")
                            } else {
                                Toast.makeText(requireContext(), "No new users available.", Toast.LENGTH_SHORT).show()
                                 displayCurrentUser()
                            }
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Failed to fetch suggested users: ${response.code()}" +
                                (errorBody?.let { " - $it" } ?: "")
                        Log.e("SuggestedUsers", errorMessage)
                        if (suggestedUsersList.isEmpty()) {
                            clearUserDisplay("Failed to load users: ${response.code()}")
                        } else {
                            Toast.makeText(requireContext(), "Error fetching users: ${response.code()}", Toast.LENGTH_SHORT).show()
                            displayCurrentUser()
                        }
                    }
                }
            } catch (e: CancellationException) {
                Log.d("SuggestedUsers", "Fetch cancelled: ${e.message}")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Error fetching suggested users: ${e.message}"
                    Log.e("SuggestedUsers", errorMessage, e)
                    Toast.makeText(
                        requireContext(),
                        "Network error fetching users: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    if (suggestedUsersList.isEmpty()) {
                        clearUserDisplay("Network error: ${e.message}")
                    } else {
                        displayCurrentUser()
                    }
                }
            } finally {
                isLoading = false
                withContext(Dispatchers.Main) {
                    setLoadingState(false)
                }
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        btnLike.isEnabled = enabled
        btnDislike.isEnabled = enabled
        btnSkip.isEnabled = enabled
    }

    private fun handleInteraction(interactionType: String) {
        if (suggestedUsersList.isEmpty() || currentUserIndex >= suggestedUsersList.size) {
            Log.w("Interaction", "No current user to interact with or index out of bounds. Attempting to display next/fetch.")
            displayNextUser()
            return
        }

        val currentUser = suggestedUsersList[currentUserIndex]
        val interactedUserId = currentUser.id

        Log.d("Interaction", "Attempting to send interaction: user ID ${interactedUserId}, type $interactionType")

        setButtonsEnabled(false)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestBody = InteractionRequest(interactedUserId, interactionType)
                val response = RetrofitClient.apiService.sendInteraction(requestBody)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val interactionResponse = response.body()
                        Log.d("Interaction", "Interaction sent successfully: Status=${interactionResponse?.status}, Message=${interactionResponse?.message}")
                        displayNextUser()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Failed to send interaction ($interactionType): Code=${response.code()}" +
                                (errorBody?.let { " - $it" } ?: "")
                        Log.e("Interaction", errorMessage)
                        Toast.makeText(
                            requireContext(),
                            "Error sending interaction: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                        displayNextUser()
                    }
                }
            } catch (e: CancellationException) {
                Log.d("Interaction", "Interaction send cancelled: ${e.message}")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Network error sending interaction: ${e.message}"
                    Log.e("Interaction", errorMessage, e)
                    Toast.makeText(
                        requireContext(),
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    displayNextUser()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    setButtonsEnabled(true)
                }
            }
        }
    }

    private fun displayCurrentUser() {
        if (suggestedUsersList.isNotEmpty() && currentUserIndex < suggestedUsersList.size) {
            val userProfileResponse = suggestedUsersList[currentUserIndex]
            Log.d("HomeFragment", "Displaying user: ${userProfileResponse.username}")
            Log.d("HomeFragment", "Full UserProfileResponse: $userProfileResponse")
            setLoadingState(false)

            userProfileResponse.profile?.let { profileData ->
                updateUIWithProfileData(profileData, userProfileResponse.username)
            } ?: run {
                Log.e("HomeFragment", "Profile data is null for user: ${userProfileResponse.username}")
                nameAgeTextView.text = "${userProfileResponse.username} | Data Missing"
                profileImageView.setImageResource(R.drawable.default_profile)
            }
            setButtonsEnabled(true)
        } else {
            clearUserDisplay("No more users. Please try again later!")
        }
    }

    private fun updateUIWithProfileData(profileData: ProfileDataResponse, username: String) {
        Log.d("HomeFragment", "ProfileData received by function: $profileData")

        val firstName = profileData.firstName.orEmpty()
        val lastName = profileData.lastName.orEmpty()
        val dateOfBirth = profileData.dateOfBirth.orEmpty()
        val imageUrl = profileData.profileImageUrl.orEmpty()

        val age = calculateAge(dateOfBirth)

        if (imageUrl.isBlank()) {
            profileImageView.setImageResource(R.drawable.default_profile)
        } else {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .into(profileImageView)
        }

        val fullName = when {
            firstName.isNotBlank() && lastName.isNotBlank() -> "$firstName $lastName"
            firstName.isNotBlank() -> firstName
            lastName.isNotBlank() -> lastName
            else -> username
        }

        nameAgeTextView.text = "$fullName | ${age}Y"
    }

    fun displayNextUser() {
        currentUserIndex++
        if (currentUserIndex < suggestedUsersList.size) {
            displayCurrentUser()
        } else {
            Log.d("HomeFragment", "End of current list, fetching more users...")
            fetchSuggestedUsers(currentPage)
            setLoadingState(true, "Fetching more users...")
        }
    }

    private fun clearUserDisplay(message: String = "No More Users") {
        profileImageView.visibility = View.VISIBLE
        profileImageView.setImageResource(R.drawable.default_profile)

        nameAgeTextView.visibility = View.VISIBLE
        nameAgeTextView.text = message
        setButtonsEnabled(false)
        loadingProgressBar.visibility = View.GONE
        loadingTextView.visibility = View.GONE
    }

    private fun calculateAge(dob: String): Int {
        if (dob.isEmpty()) return 0
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val birthDate = LocalDate.parse(dob, formatter)
            val today = LocalDate.now()
            today.year - birthDate.year -
                    if (today.dayOfYear < birthDate.dayOfYear) 1 else 0
        } catch (e: Exception) {
            Log.e("AgeCalc", "Error parsing DOB '$dob': ${e.message}")
            0
        }
    }

}