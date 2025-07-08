package com.example.datingapp.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.datingapp.models.KindOfDate
import com.example.datingapp.R
import com.example.datingapp.adapters.InterestAdapter
import com.example.datingapp.models.RegisterConstants
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class signuptype_fragment : Fragment() {

    private lateinit var backBtn: LinearLayout
    private lateinit var nextButton: Button
    private lateinit var interestsRecyclerView: RecyclerView
    private lateinit var interestAdapter: InterestAdapter
    private lateinit var skipButton: TextView
    val interestList = mutableListOf(
        KindOfDate("1", "Long-Term", R.drawable.long_term_icon),
        KindOfDate("2", "Short-Term", R.drawable.short_term_icon),
        KindOfDate("3", "Date-to-marry", R.drawable.date_to_marry_icon),
        KindOfDate("4", "Make-new-friend", R.drawable.make_new_friend_icon)
    )
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signuptype_fragment, container, false)

        backBtn = view.findViewById(R.id.backBtn)
        nextButton = view.findViewById(R.id.next_button)
        interestsRecyclerView = view.findViewById(R.id.interestsRecyclerView)
        skipButton = view.findViewById(R.id.skip)

        interestsRecyclerView.layoutManager = LinearLayoutManager(context)
            interestAdapter = InterestAdapter(interestList) { selectedInterest ->

            }
        interestsRecyclerView.adapter = interestAdapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPrefs = requireContext().getSharedPreferences(RegisterConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val savedInterestId = sharedPrefs.getString(RegisterConstants.KEY_KIND_OF_DATE_LOOKING_FOR, null) // Use 'null' or an appropriate default ID
        if (!savedInterestId.isNullOrEmpty()) {
            interestAdapter.setActiveInterest(savedInterestId)
        } else {
        }
        backBtn.setOnClickListener {
            val nextFragment = signupgender_fragment()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, nextFragment)
                .addToBackStack(null)
                .commit()
        }
        skipButton.setOnClickListener {
            val nextFragment = signupimage_fragment()
            val sharedPrefs = requireContext().getSharedPreferences(RegisterConstants.PREFS_NAME,Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                putString(RegisterConstants.KEY_KIND_OF_DATE_LOOKING_FOR, "5")
                apply()
            }
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
                .replace(R.id.fragment_container, nextFragment)
                .addToBackStack(null)
                .commit()
        }

        nextButton.setOnClickListener {
            val selectedInterest = interestAdapter.getSelectedInterest()

            if (selectedInterest != null) {
                val nextFragment = signupimage_fragment()
                val sharedPrefs = requireContext().getSharedPreferences(RegisterConstants.PREFS_NAME,Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    putString(RegisterConstants.KEY_KIND_OF_DATE_LOOKING_FOR, selectedInterest.id)
                    apply()
                }
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                    .replace(R.id.fragment_container, nextFragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Validation Error")
                    .setMessage("Please select your date type.")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }
}