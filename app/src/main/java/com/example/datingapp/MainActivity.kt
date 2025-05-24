package com.example.datingapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.os.Handler

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragmentManager = supportFragmentManager

        // Create and add the StartingScreenFragment initially
        val startingScreenFragment = starting_screen()
        val initialTransaction = fragmentManager.beginTransaction()
        initialTransaction.add(R.id.fragment_container, startingScreenFragment)
        initialTransaction.commit()

        // Create a Handler to delay the fragment replacement
        val handler = Handler()
        handler.postDelayed({
            // Create the RegisterFragment
            val registerFragment = register_fragment()

            // Begin a new FragmentTransaction to replace the StartingScreenFragment
            val transaction = fragmentManager.beginTransaction()
            transaction.setCustomAnimations(
                R.anim.fade_in,  // Animation for the entering RegisterFragment
                R.anim.fade_out // Animation for the exiting StartingScreenFragment
            )
            transaction.replace(R.id.fragment_container, registerFragment)
            transaction.addToBackStack(null) // Optional: Add to back stack
            transaction.commit()
        }, 3000) // Delay in milliseconds (3000ms = 3 seconds)
    }
}