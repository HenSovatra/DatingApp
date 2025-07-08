package com.example.datingapp.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.datingapp.R
import com.example.datingapp.api.RetrofitClient
import com.example.datingapp.fragments.chat_fragment
import com.example.datingapp.fragments.date_fragment
import com.example.datingapp.fragments.home_fragment
import com.example.datingapp.fragments.notification_fragment
import com.example.datingapp.fragments.setting_fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yourpackage.yourapp.auth.SessionManager // Ensure this import is correct
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Define the interface for profile updates.
// This interface allows EditProfileFragment to notify HomeActivity
// when a profile update has successfully occurred.
interface OnProfileUpdateListener {
    fun onProfileUpdated()
}

class HomeActivity : AppCompatActivity(), OnProfileUpdateListener { // HomeActivity implements the interface
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var profileImage : CircleImageView

    // Launcher for requesting POST_NOTIFICATIONS permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Permission", "POST_NOTIFICATIONS permission granted")
        } else {
            Log.d("Permission", "POST_NOTIFICATIONS permission denied")
            showNotificationPermissionRationale()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home) // Set the main layout for the activity

        // Initialize RetrofitClient (ensure it's done before any API calls)
        RetrofitClient.init(this)

        // Request notification permission if needed for Android 13+
        requestNotificationPermission()

        // Initialize BottomNavigationView with error handling
        try {
            bottomNavigationView = findViewById(R.id.bottom_navigation_view)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: BottomNavigationView not found with the specified ID!", Toast.LENGTH_LONG).show()
            return
        }

        // Initialize profile image view from the header layout
        // Assuming 'mainHeader' is an included layout or the ConstraintLayout itself
        val mainHeader = findViewById<View>(R.id.mainHeader)
        profileImage = mainHeader.findViewById(R.id.imageViewProfile)

        // Load the initial fragment (home_fragment) if it's the first time creating the activity
        if (savedInstanceState == null) {
            loadFragment(home_fragment())
            handleIntent(intent) // Handle any incoming intents (e.g., from notifications)
        }

        // Fetch the user's profile data and image when the activity starts
        fetchUserProfile()

        // Set up listener for BottomNavigationView item selections
        bottomNavigationView.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.navigation_home -> home_fragment()
                R.id.navigation_date -> date_fragment()
                R.id.navigation_chat -> chat_fragment()
                R.id.navigation_notification -> notification_fragment()
                R.id.navigation_setting -> setting_fragment()
                else -> return@setOnItemSelectedListener false // Return false if no fragment matches
            }

            loadFragment(selectedFragment) // Load the selected fragment
            true // Indicate that the item selection has been handled
        }
    }

    /**
     * Handles incoming intents, particularly for notifications.
     * @param intent The intent that started this activity.
     */
    private fun handleIntent(intent: Intent) {
        if (intent.action == "com.example.datingapp.VIEW_NOTIFICATION") {
            val notificationType = intent.getStringExtra("notification_type")
            if(notificationType=="date_request" || notificationType=="chat"){
                loadFragment(notification_fragment()) // Load notification fragment for specific types
            }
        }
    }

    /**
     * Requests POST_NOTIFICATIONS permission for Android 13 (API 33) and above.
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("Permission", "POST_NOTIFICATIONS permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.d("Permission", "Showing rationale for POST_NOTIFICATIONS permission")
                    showNotificationPermissionRationale()
                }
                else -> {
                    Log.d("Permission", "Requesting POST_NOTIFICATIONS permission")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d("Permission", "POST_NOTIFICATIONS permission not required for API < 33")
        }
    }

    /**
     * Returns the currently active fragment in the fragment_container.
     * @return The current Fragment or null if none is attached.
     */
    fun getCurrentFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.fragment_container)
    }

    /**
     * Displays a Toast message explaining why notification permission is needed.
     */
    private fun showNotificationPermissionRationale() {
        Toast.makeText(this, "Please enable notifications in app settings to receive date requests.", Toast.LENGTH_LONG).show()
    }

    /**
     * Loads a specified fragment into the fragment_container.
     * Prevents reloading the same fragment if it's already active.
     * @param fragment The Fragment to load.
     */
    private fun loadFragment(fragment: Fragment) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        // Check if the fragment to be loaded is already the current one
        if (currentFragment != null && currentFragment::class == fragment::class) {
            val fragmentName = fragment::class.simpleName ?: "Unknown Fragment"
            Log.d("Navigation", "Already on $fragmentName. No navigation needed.")
            return
        }

        // Perform the fragment transaction
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitNow() // Use commitNow() for immediate execution

        val fragmentName = fragment::class.simpleName ?: "Unknown Fragment"
        Log.i("Navigation", "Navigated to: $fragmentName")
    }

    /**
     * Handles the Up button (back arrow) in the action bar.
     * Currently just shows a Toast.
     */
    override fun onSupportNavigateUp(): Boolean {
        Toast.makeText(this, "Search icon clicked (Navigation Icon)", Toast.LENGTH_SHORT).show()
        return true
    }

    /**
     * Handles options menu item selections.
     * @param item The selected MenuItem.
     * @return True if the event was handled, false otherwise.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Add custom menu item handling here if needed
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Fetches the user's profile data from the backend and updates the profile image.
     */
    private fun fetchUserProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionManager = SessionManager(this@HomeActivity)
                val userId = sessionManager.getUserId()
                val authToken = sessionManager.getAuthToken()

                // Make API call to get user profile
                val response = RetrofitClient.userService.getUserById(userId, authToken)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val user = response.body()
                        val imageUrl = user?.profile?.profileImageUrl // Access profileImageUrl from nested profile
                        if (imageUrl.isNullOrEmpty()) {
                            profileImage.setImageResource(R.drawable.defaultpfp) // Set default if URL is empty
                        } else {
                            // Load image using Glide
                            Glide.with(profileImage.context)
                                .load(imageUrl)
                                .placeholder(R.drawable.defaultpfp) // Placeholder while loading
                                .error(R.drawable.defaultpfp) // Error image if loading fails
                                .into(profileImage) // Set image to imageViewProfile
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Failed to fetch user profile: ${response.code()}" +
                                (errorBody?.let { " - $it" } ?: "")
                        Log.e("HomeActivity", errorMessage)
                        Toast.makeText(this@HomeActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Error fetching user profile: ${e.message}"
                    Log.e("HomeActivity", errorMessage, e)
                    Toast.makeText(this@HomeActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Implementation of the OnProfileUpdateListener interface.
     * This method is called by EditProfileFragment upon a successful profile update.
     */
    override fun onProfileUpdated() {
        Log.d("HomeActivity", "Profile updated callback received. Refreshing profile image.")
        fetchUserProfile() // Call fetchUserProfile to refresh the image
    }
}