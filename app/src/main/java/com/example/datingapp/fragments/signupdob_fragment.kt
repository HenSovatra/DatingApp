package com.example.datingapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.app.DatePickerDialog
import android.content.Context
import com.example.datingapp.R
import com.example.datingapp.models.RegisterConstants
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException

class signupnamedob_fragment : Fragment() {


    private  lateinit var BtnBack: LinearLayout;
    private  lateinit var BtnNext: Button;
    private lateinit var selectedDateTextView: TextView
    private val calendar = Calendar.getInstance()
    private  var formattedDate: String? = null;

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signupdob_fragment, container, false)
        BtnBack = view.findViewById(R.id.backBtn);
        BtnNext = view.findViewById(R.id.next_button)
        selectedDateTextView = view.findViewById(R.id.selectedDateTextView)

        selectedDateTextView.setOnClickListener {
            showDatePickerDialog()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPrefs = requireContext().getSharedPreferences(RegisterConstants.PREFS_NAME,Context.MODE_PRIVATE)
        selectedDateTextView.setText(sharedPrefs.getString(RegisterConstants.KEY_DATE_OF_BIRTH_DISPLAY, ""))
        formattedDate = sharedPrefs.getString(RegisterConstants.KEY_DATE_OF_BIRTH, "")
        BtnBack.setOnClickListener {
            val signupname = signupname_fragment()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, signupname)
                .addToBackStack(null)
                .commit()
        }
        BtnNext.setOnClickListener nextButtonClickListener@{
            if (formattedDate.isNullOrBlank()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Validation Error")
                    .setMessage("Please select your date of birth.")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                return@nextButtonClickListener
            }

            try {
                val birthDate = LocalDate.parse(formattedDate)
                val currentDate = LocalDate.now()
                val age = Period.between(birthDate, currentDate).years

                if (age < 18) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Validation Error")
                        .setMessage("You must be at least 18 years old to use this app.")
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                    return@nextButtonClickListener
                }
            } catch (e: DateTimeParseException) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Error")
                    .setMessage("Invalid date format. Please select your date of birth again.")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                e.printStackTrace()
                return@nextButtonClickListener
            }

            val sharedPrefs = requireContext().getSharedPreferences(
                RegisterConstants.PREFS_NAME,
                Context.MODE_PRIVATE
            )
            with(sharedPrefs.edit()) {
                putString(RegisterConstants.KEY_DATE_OF_BIRTH, formattedDate)
                putString(RegisterConstants.KEY_DATE_OF_BIRTH_DISPLAY, selectedDateTextView.text.toString())
                apply()
            }

            val nextFragment = signupgender_fragment()
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

    private fun showDatePickerDialog() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                calendar.set(Calendar.YEAR, selectedYear)
                calendar.set(Calendar.MONTH, selectedMonth)
                calendar.set(Calendar.DAY_OF_MONTH, selectedDayOfMonth)
                updateDateInView()
            },
            year,
            month,
            day
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun updateDateInView() {
        val myFormat = "dd    |   MMMM   |    yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        selectedDateTextView.text = sdf.format(calendar.time)
        formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time);
    }
}