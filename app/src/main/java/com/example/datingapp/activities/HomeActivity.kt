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
import com.yourpackage.yourapp.auth.SessionManager
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var profileImage : CircleImageView
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
        setContentView(R.layout.activity_home)
        RetrofitClient.init(this)
        requestNotificationPermission()
        try {
            bottomNavigationView = findViewById(R.id.bottom_navigation_view)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: BottomNavigationView not found with the specified ID!", Toast.LENGTH_LONG).show()
            return
        }
        val mainHeader = findViewById<View>(R.id.mainHeader)
        //searchTab = mainHeader.findViewById(R.id.searchTab)
        profileImage = mainHeader.findViewById(R.id.imageViewProfile)
        //notificationIcon = mainHeader.findViewById(R.id.notificationIcon)
        if (savedInstanceState == null) {
            loadFragment(home_fragment()) // Initial fragment load
            handleIntent(intent)
        }

        fetchUserProfile()
        bottomNavigationView.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.navigation_home -> home_fragment()
                R.id.navigation_date -> date_fragment()
                R.id.navigation_chat -> chat_fragment()
                R.id.navigation_notification -> notification_fragment()
                R.id.navigation_setting -> setting_fragment()
                else -> return@setOnItemSelectedListener false
            }

            loadFragment(selectedFragment)
            true
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == "com.example.datingapp.VIEW_NOTIFICATION") {
            val notificationType = intent.getStringExtra("notification_type")
            if(notificationType=="date_request"){
                loadFragment(notification_fragment())
            }else if(notificationType=="chat"){
                loadFragment(notification_fragment())
            }
        } else {
        }
    }

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
                    // Request the permission directly
                    Log.d("Permission", "Requesting POST_NOTIFICATIONS permission")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d("Permission", "POST_NOTIFICATIONS permission not required for API < 33")
        }
    }

    private fun showNotificationPermissionRationale() {
        Toast.makeText(this, "Please enable notifications in app settings to receive date requests.", Toast.LENGTH_LONG).show()
    }

    private fun loadFragment(fragment: Fragment) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment != null && currentFragment::class == fragment::class) {
            val fragmentName = fragment::class.simpleName ?: "Unknown Fragment"
            Log.d("Navigation", "Already on $fragmentName. No navigation needed.")
            return
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitNow()

        val fragmentName = fragment::class.simpleName ?: "Unknown Fragment"
        Log.i("Navigation", "Navigated to: $fragmentName")

        doSomethingBasedOnCurrentFragment()
    }

    override fun onSupportNavigateUp(): Boolean {
        Toast.makeText(this, "Search icon clicked (Navigation Icon)", Toast.LENGTH_SHORT).show()
        // If you had a DrawerLayout, you'd open it here
        return true
    }
    fun getCurrentDisplayedFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.fragment_container)
    }
    fun doSomethingBasedOnCurrentFragment() {
//        val currentFragment = getCurrentDisplayedFragment()
//        when (currentFragment) {
//            is home_fragment -> {
//                searchTab.visibility = View.VISIBLE
//                notificationIcon.visibility = View.VISIBLE
//            }
//            is chat_fragment -> {
//
//                notificationIcon.visibility = View.GONE
//                searchTab.visibility = View.VISIBLE
//            }
//            else -> {
//                searchTab.visibility = View.GONE
//                notificationIcon.visibility = View.GONE
//            }
//        }
    }
    // For handling overflow menu items if you had any in top_app_bar_menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Handle other generic menu items if they exist
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun fetchUserProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionManager = SessionManager(this@HomeActivity)
                val userId = sessionManager.getUserId();
                val authToken = sessionManager.getAuthToken()
                val response = RetrofitClient.userService.getUserById(userId, authToken)
                withContext(Dispatchers.Main) { // Switch back to Main thread for UI updates
                    if (response.isSuccessful && response.body() != null) {
                        val user = response.body()
                        val imageUrl = user?.profile?.profileImageUrl;
                        if (imageUrl.isNullOrEmpty()) {
                            profileImage.setImageResource(R.drawable.default_profile);
                        } else {
                            Glide.with(profileImage.context)
                                .load(imageUrl)
                                .placeholder(R.drawable.defaultpfp)
                                .error(R.drawable.defaultpfp)
                                .into(profileImage);
                        }

                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Failed to fetch user profile: ${response.code()}" +
                                (errorBody?.let { " - $it" } ?: "")
                        Log.e("YourActivityName", errorMessage) // Change tag
                        Toast.makeText(this@HomeActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Error fetching user profile: ${e.message}"
                    Log.e("YourActivityName", errorMessage, e) // Change tag
                    Toast.makeText(this@HomeActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}