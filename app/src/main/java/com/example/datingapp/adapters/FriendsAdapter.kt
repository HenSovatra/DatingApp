package com.example.datingapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.datingapp.R
import com.example.datingapp.models.ConversationListItem

class FriendsAdapter(
    private val conversationList: MutableList<ConversationListItem>,
    private val onItemClick: (ConversationListItem) -> Unit
) : RecyclerView.Adapter<FriendsAdapter.ConversationViewHolder>() {

    class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val lastMessageTextView: TextView = itemView.findViewById(R.id.lastMessageTextView)
        val timeAgoTextView: TextView = itemView.findViewById(R.id.timeAgoTextView)
        val unreadIndicator: View = itemView.findViewById(R.id.unreadIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val conversation = conversationList[position]
        holder.itemView.setOnClickListener {
            onItemClick(conversation)
        }

        holder.nameTextView.text = conversation.otherParticipant?.fullName ?: "Unknown User"
        holder.lastMessageTextView.text = conversation.lastMessageContent ?: "No messages"
        holder.timeAgoTextView.text = conversation.timeAgo ?: ""

        val imageUrl = conversation.otherParticipant?.displayPictureUrl
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.defaultpfp)
                .error(R.drawable.defaultpfp)
                .circleCrop()
                .into(holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.defaultpfp)
        }

        if (conversation.hasUnread) {
            holder.unreadIndicator.visibility = View.VISIBLE
        } else {
            holder.unreadIndicator.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return conversationList.size
    }
}