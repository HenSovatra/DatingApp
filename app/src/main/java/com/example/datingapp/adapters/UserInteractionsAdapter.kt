package com.example.datingapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.datingapp.R
import com.example.datingapp.models.UserInteractionNotification

class UserInteractionsAdapter(
    private val interactionsList: MutableList<UserInteractionNotification>,
    private val onItemClick: (UserInteractionNotification) -> Unit
) : RecyclerView.Adapter<UserInteractionsAdapter.InteractionViewHolder>() {

    class InteractionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userProfileImage: ImageView = itemView.findViewById(R.id.userProfileImage)
        val interactionTextView: TextView = itemView.findViewById(R.id.interactionTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InteractionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_interaction, parent, false)
        return InteractionViewHolder(view)
    }

    override fun onBindViewHolder(holder: InteractionViewHolder, position: Int) {
        val interaction = interactionsList[position]

        // Load profile image
        Glide.with(holder.itemView.context)
            .load(interaction.firstLikerImageUrl)
            .placeholder(R.drawable.defaultpfp)
            .error(R.drawable.defaultpfp)
            .into(holder.userProfileImage)

        // Set interaction text
        val interactionText = if (interaction.otherLikersCount > 0) {
            "${interaction.firstLikerName} and ${interaction.otherLikersCount} others like you."
        } else {
            "${interaction.firstLikerName} likes you."
        }
        holder.interactionTextView.text = interactionText

        holder.itemView.setOnClickListener { onItemClick(interaction) }
    }

    override fun getItemCount(): Int = interactionsList.size

    fun updateList(newList: List<UserInteractionNotification>) {
        interactionsList.clear()
        interactionsList.addAll(newList)
        notifyDataSetChanged()
    }
}