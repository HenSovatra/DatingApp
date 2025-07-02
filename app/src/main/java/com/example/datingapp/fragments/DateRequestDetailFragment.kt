package com.example.datingapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.datingapp.R
import com.example.datingapp.models.DateRequestResponse // Make sure this import is correct
import com.google.gson.Gson // Make sure you have Gson dependency if passing complex objects

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

class DateRequestDetailFragment : Fragment() {

    private lateinit var requesterAvatar: ImageView
    private lateinit var requesterName: TextView
    private lateinit var dateType: TextView
    private lateinit var message: TextView
    private lateinit var status: TextView
    private lateinit var dateTime: TextView
    private lateinit var location: TextView
    private lateinit var backButton: ImageButton

    companion object {
        private const val ARG_DATE_REQUEST = "date_request_arg"

        fun newInstance(dateRequest: DateRequestResponse): DateRequestDetailFragment {
            val fragment = DateRequestDetailFragment()
            val args = Bundle()
            args.putString(ARG_DATE_REQUEST, Gson().toJson(dateRequest))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_date_request_detail, container, false)

        requesterAvatar = view.findViewById(R.id.detailRequesterAvatar)
        requesterName = view.findViewById(R.id.detailRequesterName)
        dateType = view.findViewById(R.id.detailDateType)
        message = view.findViewById(R.id.detailMessage)
        status = view.findViewById(R.id.detailStatus)
        dateTime = view.findViewById(R.id.detailDateTime)
        location = view.findViewById(R.id.detailLocation)
        backButton = view.findViewById(R.id.backButton)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString(ARG_DATE_REQUEST)?.let { json ->
            val dateRequest = Gson().fromJson(json, DateRequestResponse::class.java)
            displayRequestDetails(dateRequest)
        }

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun displayRequestDetails(request: DateRequestResponse) {
        val requesterFullName = "${request.requester.profile?.firstName.orEmpty()} ${request.requester.profile?.lastName.orEmpty()}".trim()
        requesterName.text = if (requesterFullName.isNotEmpty()) requesterFullName else request.requester.username

        Glide.with(this)
            .load(request.requester.profile?.profileImageUrl)
            .placeholder(R.drawable.defaultpfp)
            .error(R.drawable.defaultpfp)
            .into(requesterAvatar)

        dateType.text = "Date Type: ${request.dateTypeName}"
        message.text = "Message: ${request.message ?: "No message"}"
        status.text = "Status: ${request.status.capitalize(Locale.ROOT)}"
        location.text = "Location: ${request.location ?: "N/A"}"

        request.dateTime?.let { dtString ->
            try {
                val formatterInput = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                val dateTimeObj = LocalDateTime.parse(dtString, formatterInput)
                val formatterOutput = DateTimeFormatter.ofPattern("MMM d,yyyy 'at' h:mm a", Locale.getDefault())
                dateTime.text = "Date/Time: ${dateTimeObj.format(formatterOutput)}"
            } catch (e: DateTimeParseException) {
                dateTime.text = "Date/Time: Invalid Date"
                e.printStackTrace()
            }
        } ?: run {
            dateTime.text = "Date/Time: Not set"
        }
    }
}