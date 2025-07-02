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

class signupname_fragment : Fragment() {


    private  lateinit var BtnBack: LinearLayout;
    private  lateinit var BtnNext: Button;
    private lateinit var TxtFirstName : EditText;
    private lateinit var TxtLastName : EditText;

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signupname_fragment, container, false)
        BtnBack = view.findViewById(R.id.backBtn);
        BtnNext = view.findViewById(R.id.next_button)
        TxtFirstName = view.findViewById(R.id.firstname)
        TxtLastName = view.findViewById(R.id.lastname)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPrefs = requireContext().getSharedPreferences(RegisterConstants.PREFS_NAME,Context.MODE_PRIVATE)
        TxtFirstName.setText(sharedPrefs.getString(RegisterConstants.KEY_FIRST_NAME, ""))
        TxtLastName.setText(sharedPrefs.getString(RegisterConstants.KEY_LAST_NAME, ""))
        BtnBack.setOnClickListener {

            val prevFragment = signup_fragment()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, prevFragment)
                .addToBackStack(null)
                .commit()
        }

        BtnNext.setOnClickListener nextButtonClickListener@{

            val nextFragment = signupnamedob_fragment()
            val firstName = TxtFirstName.text.toString().trim();
            val lastname = TxtLastName.text.toString().trim();

            if(firstName.isEmpty()){
                TxtFirstName.error = "First Name is empty."
                return@nextButtonClickListener
            }

            if(lastname.isEmpty()){
                TxtFirstName.error = "Last Name is empty."
                return@nextButtonClickListener
            }

            val sharedPrefs = requireContext().getSharedPreferences(RegisterConstants.PREFS_NAME,Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                putString(RegisterConstants.KEY_FIRST_NAME, firstName)
                putString(RegisterConstants.KEY_LAST_NAME, lastname)
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
    }
}