package com.example.datingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Assuming you are using Glide for images
import com.example.datingapp.models.Conversation

class ConversationAdapter(
    private var conversations: List<Conversation>,
    private val listener: OnConversationClickListener
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val conversation = conversations[position]

        holder.participantName.text = conversation.otherParticipant.fullName
        conversation.otherParticipant.displayPictureUrl?.let { imageUrl ->
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.defaultpfp)
                .error(R.drawable.defaultpfp)
                .into(holder.profileImage)
        } ?: run {
            holder.profileImage.setImageResource(R.drawable.defaultpfp)
        }

        holder.itemView.setOnClickListener {
            listener.onConversationClick(
                conversation.id,
                conversation.otherParticipant.fullName,
                conversation.otherParticipant.displayPictureUrl
            )
        }
    }

    override fun getItemCount(): Int {
        return conversations.size
    }

    class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val participantName: TextView = itemView.findViewById(R.id.participant_name)
    }
}