package com.example.datingapp.activities

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.datingapp.api.RetrofitClient
import com.google.firebase.FirebaseApp
import android.Manifest
import com.example.datingapp.R
import com.example.datingapp.fragments.authentication

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your MyFirebaseMessagingService) can now display notifications
            Log.d("Permission", "POST_NOTIFICATIONS permission granted")

            // You might want to re-send the FCM token to backend here,
            // though onNewToken should handle it if it changed.
            // sendFcmTokenToBackend() // If you want to force a re-registration
        } else {
            // Explain to the user why notifications are important
            Log.d("Permission", "POST_NOTIFICATIONS permission denied")
            // Optionally, guide the user to app settings
            showNotificationPermissionRationale()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(this)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)
        val fragmentManager = supportFragmentManager

        val registerFragment = authentication()

        val transaction = fragmentManager.beginTransaction()
        transaction.setCustomAnimations(
            R.anim.fade_in,  // Animation for the entering RegisterFragment
            R.anim.fade_out // Animation for the exiting StartingScreenFragment
        )
        transaction.replace(R.id.fragment_container, registerFragment)
        transaction.addToBackStack(null) // Optional: Add to back stack
        transaction.commit()
        requestNotificationPermission()
    }
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU is API 33
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    Log.d("Permission", "POST_NOTIFICATIONS permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Explain to the user why you need the permission (e.g., using a dialog)
                    // Then launch the permission request
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
            // For devices below Android 13, permission is granted by default
            Log.d("Permission", "POST_NOTIFICATIONS permission not required for API < 33")
            // No action needed other than ensuring MyFirebaseMessagingService is registered
        }
    }

    private fun showNotificationPermissionRationale() {
        // Implement a dialog or UI to explain to the user why they should enable notifications.
        // For example:
        // AlertDialog.Builder(this)
        //     .setTitle("Enable Notifications")
        //     .setMessage("Allow DatingApp to send you notifications for new date requests and messages.")
        //     .setPositiveButton("Allow") { dialog, which ->
        //         requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        //     }
        //     .setNegativeButton("No Thanks", null)
        //     .show()
        Toast.makeText(this, "Please enable notifications in app settings to receive date requests.", Toast.LENGTH_LONG).show()
        // You might also want to provide a button that takes them directly to app settings:
        // val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        // val uri = Uri.fromParts("package", packageName, null)
        // intent.data = uri
        // startActivity(intent)
    }
}