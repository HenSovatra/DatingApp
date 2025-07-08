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
            Log.d("Permission", "POST_NOTIFICATIONS permission granted")
        } else {
            Log.d("Permission", "POST_NOTIFICATIONS permission denied")
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
            R.anim.fade_in,
            R.anim.fade_out
        )
        transaction.replace(R.id.fragment_container, registerFragment)
        transaction.addToBackStack(null)
        transaction.commit()
        requestNotificationPermission()
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
}