// app/src/main/java/com/example/datingapp/adapters/UserInteractionListAdapter.kt

package com.example.datingapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.datingapp.R
import com.example.datingapp.fragments.UserInteractionListFragment
import com.example.datingapp.models.UserInteractionItem
import com.bumptech.glide.Glide

class UserInteractionListAdapter(
    private val listType: String,
    private var users: List<UserInteractionItem>,
    private val onActionButtonClick: (userId: Int, action: String) -> Unit
) : RecyclerView.Adapter<UserInteractionListAdapter.UserInteractionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserInteractionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_interactions, parent, false)
        return UserInteractionViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserInteractionViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    fun updateUsers(newUsers: List<UserInteractionItem>) {
        users = newUsers
        notifyDataSetChanged()
    }

    inner class UserInteractionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.userProfileImage)
        private val userNameText: TextView = itemView.findViewById(R.id.userName)
        private val statusText: TextView = itemView.findViewById(R.id.status_text)
        private val actionButton: Button = itemView.findViewById(R.id.actionButton)
        private val userAgeText: TextView = itemView.findViewById(R.id.user_age_text)

        fun bind(user: UserInteractionItem) {
            userNameText.text = user.profile?.fullName ?: "N/A"
            userAgeText.text = user.profile?.age?.let { "$it years old" } ?: "N/A"

            Glide.with(itemView.context)
                .load(user.profile?.profileImageUrl)
                .placeholder(R.drawable.defaultpfp)
                .error(R.drawable.defaultpfp)
                .into(profileImage)

            when (listType) {
                UserInteractionListFragment.TYPE_MUTUAL -> {
                    statusText.visibility = View.GONE
                    actionButton.visibility = View.VISIBLE
                    actionButton.text = "Unlike"
                    actionButton.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.primary))
                    actionButton.setOnClickListener {
                        onActionButtonClick(user.id, "unlike")
                    }
                }
                UserInteractionListFragment.TYPE_OUTGOING -> {
                    // Logic for OutgoingLikesView
                    actionButton.visibility = View.VISIBLE
                    actionButton.text = "Unlike"

                    if (user.isMutualMatch == true) {
                        statusText.visibility = View.GONE
                     } else {
                        statusText.visibility = View.VISIBLE
                        statusText.text = "Pending Match"
                        statusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.info))
                    }
                    actionButton.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.primary)) // Set the color consistently
                    actionButton.setOnClickListener {
                        onActionButtonClick(user.id, "unlike")
                    }
                }
                UserInteractionListFragment.TYPE_INCOMING -> {
                    statusText.visibility = View.VISIBLE
                    statusText.text = "Liked you!"
                    statusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary))
                    actionButton.visibility = View.VISIBLE
                    actionButton.text = "Like Back"
                    actionButton.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.primary))
                    actionButton.setOnClickListener {
                        onActionButtonClick(user.id, "like_back")
                    }
                }
                else -> {
                    statusText.visibility = View.GONE
                    actionButton.visibility = View.GONE
                }
            }
        }
    }
}