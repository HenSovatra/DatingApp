package com.example.datingapp.adapters // Make sure this matches your package structure

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Assuming you use Glide for image loading
import com.example.datingapp.R
import com.example.datingapp.models.UserInteractionItem // Your UserInteractionItem data class
import de.hdodenhof.circleimageview.CircleImageView

class OverlaySearchResultAdapter(
    private var users: List<UserInteractionItem>,
    private val onItemClick: (UserInteractionItem) -> Unit // Callback for when a user is clicked
) : RecyclerView.Adapter<OverlaySearchResultAdapter.SearchResultViewHolder>() {

    fun updateUsers(newUsers: List<UserInteractionItem>) {
        users = newUsers
        notifyDataSetChanged() // For simple list updates
        // For better performance with large lists, consider using DiffUtil
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_interaction_search_result, parent, false)
        return SearchResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = users.size

    inner class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userName: TextView = itemView.findViewById(R.id.search_result_user_name)
        private val profileImage: CircleImageView = itemView.findViewById(R.id.search_result_profile_image)

        fun bind(user: UserInteractionItem) {
            userName.text = user.profile?.fullName ?: user.username // Display full name or username
            user.profile?.profileImageUrl?.let { url ->
                Glide.with(itemView.context)
                    .load(url)
                    .placeholder(R.drawable.defaultpfp) // Default placeholder image
                    .error(R.drawable.defaultpfp) // Image to show on error
                    .into(profileImage)
            } ?: profileImage.setImageResource(R.drawable.defaultpfp) // Fallback if no URL

            itemView.setOnClickListener {
                onItemClick(user) // Trigger the callback when an item is clicked
            }
        }
    }
}