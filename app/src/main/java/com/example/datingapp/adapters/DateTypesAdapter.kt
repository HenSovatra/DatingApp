package com.example.datingapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.datingapp.R
import com.example.datingapp.models.DateTypeItem

class DateTypesAdapter(
    private val dateTypes: List<DateTypeItem>,
    private val onItemClick: (DateTypeItem) -> Unit
) : RecyclerView.Adapter<DateTypesAdapter.DateTypeViewHolder>() {

    class DateTypeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconImageView: ImageView = itemView.findViewById(R.id.dateTypeIcon)
        val nameTextView: TextView = itemView.findViewById(R.id.dateTypeName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateTypeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_date_type, parent, false)
        return DateTypeViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateTypeViewHolder, position: Int) {
        val dateType = dateTypes[position]

        holder.nameTextView.text = dateType.name
        holder.iconImageView.setImageResource(dateType.drawableResId)

        holder.itemView.setOnClickListener {
            onItemClick(dateType)
        }
    }

    override fun getItemCount(): Int {
        return dateTypes.size
    }
}