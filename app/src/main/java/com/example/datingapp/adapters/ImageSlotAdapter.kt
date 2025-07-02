package com.example.datingapp.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.datingapp.models.ImageItem
import com.example.datingapp.R
import com.google.android.material.card.MaterialCardView

class ImageSlotAdapter(
    private val imageItems: MutableList<ImageItem>,
    private val onAddImageClick: (position: Int) -> Unit,
    private val onDeleteImageClick: (position: Int) -> Unit
) : RecyclerView.Adapter<ImageSlotAdapter.ImageSlotViewHolder>() {

    inner class ImageSlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.imageSlotCardView)
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val addIcon: ImageView = itemView.findViewById(R.id.addIcon)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    if (imageItems[position].isAddButton) {
                        onAddImageClick(position)
                    } else {
                        onDeleteImageClick(position)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageSlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_slot, parent, false)
        return ImageSlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageSlotViewHolder, position: Int) {
        val item = imageItems[position]

        if (item.isAddButton || item.imageUri == null) {
            holder.addIcon.visibility = View.VISIBLE
            holder.imageView.visibility = View.GONE
            holder.imageView.setImageDrawable(null)
            holder.cardView.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.gray_inactive) // Inactive border
        } else {
            holder.addIcon.visibility = View.GONE
            holder.imageView.visibility = View.VISIBLE
            holder.cardView.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.pink_active) // Active border

            Glide.with(holder.itemView.context)
                .load(item.imageUri)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_broken_image)
                .into(holder.imageView)
        }
    }

    override fun getItemCount(): Int = imageItems.size

    fun updateImageSlot(position: Int, uri: Uri?) {
        if (position < imageItems.size) {
            imageItems[position].imageUri = uri
            imageItems[position].isAddButton = (uri == null)
            notifyItemChanged(position)
        }
    }

    fun getAllImageUris(): List<Uri> {
        return imageItems.mapNotNull { it.imageUri }
    }
}