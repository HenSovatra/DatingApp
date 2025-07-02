package com.example.datingapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.datingapp.models.KindOfDate
import com.example.datingapp.R
import com.google.android.material.card.MaterialCardView

class InterestAdapter(
    private val interests: MutableList<KindOfDate>,
    private val onInterestSelected: (KindOfDate) -> Unit
) : RecyclerView.Adapter<InterestAdapter.InterestViewHolder>() {

    private var activePosition: Int = RecyclerView.NO_POSITION

    inner class InterestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.interestCardView)
        val icon: ImageView = itemView.findViewById(R.id.interestIcon)
        val name: TextView = itemView.findViewById(R.id.interestName)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val clickedItem = interests[position]

                    if (activePosition != RecyclerView.NO_POSITION && activePosition != position) {
                        interests[activePosition].isActive = false
                        notifyItemChanged(activePosition)
                    }

                    clickedItem.isActive = !clickedItem.isActive

                    if (clickedItem.isActive) {
                        activePosition = position
                    } else {
                        activePosition = RecyclerView.NO_POSITION
                    }

                    notifyItemChanged(position)

                    onInterestSelected(clickedItem)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.kindofdateitem, parent, false)
        return InterestViewHolder(view)
    }

    override fun onBindViewHolder(holder: InterestViewHolder, position: Int) {
        val interest = interests[position]

        holder.name.text = interest.name
        holder.icon.setImageResource(interest.iconResId)

        if (interest.isActive) {
            holder.cardView.setStrokeColor(ContextCompat.getColor(holder.itemView.context, R.color.pink_active))
            holder.name.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.pink_active)) // Apply tint to icon
        } else {
            holder.cardView.setStrokeColor(ContextCompat.getColor(holder.itemView.context, R.color.gray_inactive))
            holder.name.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.gray_inactive)) // Apply tint to icon
        }
    }

    override fun getItemCount(): Int = interests.size

    fun getSelectedInterest(): KindOfDate? {
        return if (activePosition != RecyclerView.NO_POSITION) {
            interests[activePosition]
        } else {
            null
        }
    }
    fun setActiveInterest(targetId: String) {
        val newActivePosition = interests.indexOfFirst { it.id == targetId }

        if (newActivePosition != RecyclerView.NO_POSITION) {
            if (activePosition != RecyclerView.NO_POSITION && activePosition != newActivePosition) {
                interests[activePosition].isActive = false
                notifyItemChanged(activePosition)
            }

            interests[newActivePosition].isActive = true
            activePosition = newActivePosition
            notifyItemChanged(activePosition)

        }
    }
}