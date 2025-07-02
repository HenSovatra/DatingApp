package com.example.datingapp.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.datingapp.R
import com.example.datingapp.models.DateRequestResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

class SentDateRequestsAdapter(
    private val dateRequests: MutableList<DateRequestResponse>,
    private val onItemClick: (DateRequestResponse) -> Unit,
    private val onCancelClick: (DateRequestResponse) -> Unit
) : RecyclerView.Adapter<SentDateRequestsAdapter.SentDateRequestViewHolder>() {

    class SentDateRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val partnerAvatar: ImageView = itemView.findViewById(R.id.partnerAvatar)
        val partnerName: TextView = itemView.findViewById(R.id.partnerName)
        val dateType: TextView = itemView.findViewById(R.id.dateType)
        val messagePreview: TextView = itemView.findViewById(R.id.messagePreview)
        val status: TextView = itemView.findViewById(R.id.status)
        val dateTime: TextView = itemView.findViewById(R.id.dateTime)
        val cancelButton: ImageButton = itemView.findViewById(R.id.cancelButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SentDateRequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sent_date_request, parent, false)
        return SentDateRequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: SentDateRequestViewHolder, position: Int) {
        val request = dateRequests[position]

        val partnerFullName = "${request.partner.profile?.firstName.orEmpty()} ${request.partner.profile?.lastName.orEmpty()}".trim()
        holder.partnerName.text = if (partnerFullName.isNotEmpty()) partnerFullName else request.partner.username

        Glide.with(holder.itemView.context)
            .load(request.partner.profile?.profileImageUrl)
            .placeholder(R.drawable.defaultpfp)
            .error(R.drawable.defaultpfp)
            .into(holder.partnerAvatar)

        holder.dateType.text = request.dateTypeName
        holder.messagePreview.text = request.message ?: "No message"
        holder.status.text = "Status: ${request.status.capitalize(Locale.ROOT)}"

        when (request.status) {
            "pending" -> holder.status.setTextColor(Color.parseColor("#FFA500"))
            "accepted" -> holder.status.setTextColor(Color.parseColor("#008000"))
            "denied", "cancelled" -> holder.status.setTextColor(Color.parseColor("#FF0000"))
            else -> holder.status.setTextColor(Color.BLACK)
        }

        request.dateTime?.let { dtString ->
            try {
                val formatterInput = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                val dateTimeObj = LocalDateTime.parse(dtString, formatterInput)
                val formatterOutput = DateTimeFormatter.ofPattern("MMM d,yyyy 'at' h:mm a", Locale.getDefault())
                holder.dateTime.text = dateTimeObj.format(formatterOutput)
            } catch (e: DateTimeParseException) {
                holder.dateTime.text = "Invalid Date"
                e.printStackTrace()
            }
        } ?: run {
            holder.dateTime.text = "Date/Time not set"
        }

        if (request.status == "pending") {
            holder.cancelButton.visibility = View.VISIBLE
        } else {
            holder.cancelButton.visibility = View.GONE
        }

        holder.cancelButton.setOnClickListener { onCancelClick(request) }
        holder.itemView.setOnClickListener { onItemClick(request) }
    }

    override fun getItemCount(): Int = dateRequests.size

    fun updateData(newData: List<DateRequestResponse>) {
        dateRequests.clear()
        dateRequests.addAll(newData)
        notifyDataSetChanged()
    }

    fun updateRequestStatus(requestId: Int, newStatus: String) {
        val index = dateRequests.indexOfFirst { it.id == requestId }
        if (index != -1) {
            val updatedRequest = dateRequests[index].copy(status = newStatus)
            dateRequests[index] = updatedRequest
            notifyItemChanged(index)
        }
    }
}