package com.example.datingapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.datingapp.databinding.FragmentStartingScreenBinding
import kotlinx.coroutines.*

class starting_screen : Fragment() {

    private var _binding: FragmentStartingScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStartingScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Delay and navigate
        CoroutineScope(Dispatchers.Main).launch {
            delay(3000) // Wait for 3 seconds
            navigateToNextFragment()
        }
    }

    private fun navigateToNextFragment() {
        val nextFragment = register_fragment() // Replace with your actual next fragment
        parentFragmentManager.beginTransaction()
            .replace(R.id.register_fragment, nextFragment)
            .addToBackStack(null) // Optional
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}