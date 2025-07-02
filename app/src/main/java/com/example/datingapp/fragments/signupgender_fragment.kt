package com.example.datingapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Context
import android.widget.*
import com.example.datingapp.R
import com.example.datingapp.models.RegisterConstants
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class signupgender_fragment : Fragment() {


    private lateinit var buttonMale: MaterialButton
    private lateinit var buttonFemale: MaterialButton
    private  lateinit var BtnBack: LinearLayout;
    private  lateinit var BtnNext: Button;


    private var selectedGender: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signupgender_fragment, container, false)

        buttonMale = view.findViewById(R.id.buttonMale)
        buttonFemale = view.findViewById(R.id.buttonFemale)

        buttonMale.setOnClickListener {
            setActiveButton(buttonMale)
            selectedGender = "Male"
        }

        buttonFemale.setOnClickListener {
            setActiveButton(buttonFemale)
            selectedGender = "Female"
        }
        BtnBack = view.findViewById(R.id.backBtn);
        BtnNext = view.findViewById(R.id.next_button)
        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPrefs = requireContext().getSharedPreferences(RegisterConstants.PREFS_NAME,Context.MODE_PRIVATE)

        selectedGender = sharedPrefs.getString(RegisterConstants.KEY_GENDER, "")
        if(selectedGender=="Male"){
            setActiveButton(buttonMale)
        }else if (selectedGender == "Female"){
            setActiveButton(buttonFemale)
        }
        BtnBack.setOnClickListener {
            val signupdob = signupnamedob_fragment()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, signupdob)
                .addToBackStack(null)
                .commit()
        }

        BtnNext.setOnClickListener nextButtonClickListener@{
            if(selectedGender.isNullOrEmpty()){
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Validation Error")
                    .setMessage("Please select your gender.")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                return@nextButtonClickListener;
            }
            val sharedPrefs = requireContext().getSharedPreferences(RegisterConstants.PREFS_NAME,Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                putString(RegisterConstants.KEY_GENDER, selectedGender)
                apply()
            }
            val nextFragment = signuptype_fragment()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
                .replace(R.id.fragment_container, nextFragment)
                .addToBackStack(null)
                .commit()
        }
    }
    private fun setActiveButton(activeButton: MaterialButton) {
        val pinkActiveColor = resources.getColor(R.color.pink_active, null)
        val grayInactiveColor = resources.getColor(R.color.gray_inactive, null)

        buttonMale.strokeColor = resources.getColorStateList(R.color.gray_inactive, null)
        buttonMale.setTextColor(grayInactiveColor)

        buttonFemale.strokeColor = resources.getColorStateList(R.color.gray_inactive, null)
        buttonFemale.setTextColor(grayInactiveColor)


        // Activate the selected button
        activeButton.strokeColor = resources.getColorStateList(R.color.pink_active, null)
        activeButton.setTextColor(pinkActiveColor)
    }
}