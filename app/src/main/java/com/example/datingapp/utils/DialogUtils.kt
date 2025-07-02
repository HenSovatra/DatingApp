package com.example.datingapp.utils // Your utility package

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.Fragment


fun Fragment.showAlertDialog(title: String, message: String) {
    if (isAdded && activity != null) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}