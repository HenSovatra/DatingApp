package com.example.datingapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.datingapp.R
import com.example.datingapp.models.Message
class MessagesAdapter(
    private val messages: MutableList<Message>,
    private val currentUserId: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val VIEW_TYPE_MESSAGE_SENT = 1
    private val VIEW_TYPE_MESSAGE_RECEIVED = 2

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.sender.id == currentUserId) {
            VIEW_TYPE_MESSAGE_SENT
        } else {
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        when (holder.itemViewType) {
            VIEW_TYPE_MESSAGE_SENT -> (holder as SentMessageViewHolder).bind(message)
            VIEW_TYPE_MESSAGE_RECEIVED -> (holder as ReceivedMessageViewHolder).bind(message)
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.text_message_sent)
        val timestampText: TextView = itemView.findViewById(R.id.text_timestamp_sent)

        fun bind(message: Message) {
            messageText.text = message.messageContent
            timestampText.text = message.timeAgo
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.text_message_received)
        val timestampText: TextView = itemView.findViewById(R.id.text_timestamp_received)
        val profileImage: ImageView = itemView.findViewById(R.id.image_message_profile)
        fun bind(message: Message) {
            messageText.text = message.messageContent
            timestampText.text = message.timeAgo

            message.sender.displayPictureUrl?.let { imageUrl ->
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.defaultpfp)
                    .error(R.drawable.defaultpfp)
                    .into(profileImage)
            } ?: run {
                profileImage.setImageResource(R.drawable.defaultpfp)
            }
        }
    }
}