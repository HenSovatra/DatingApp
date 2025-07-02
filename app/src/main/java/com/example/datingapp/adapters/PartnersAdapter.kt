package com.example.datingapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.datingapp.R
import com.example.datingapp.models.UserProfileResponse
import com.google.android.material.imageview.ShapeableImageView

class PartnersAdapter(
    private val partners: List<UserProfileResponse>,
    private val onItemClick: (UserProfileResponse) -> Unit // Callback for clicks
) : RecyclerView.Adapter<PartnersAdapter.PartnerViewHolder>() {

    private var selectedPartnerId: Int? = null

    fun setSelectedPartner(partnerId: Int?) {
        val oldSelectedId = selectedPartnerId
        selectedPartnerId = partnerId

        if (oldSelectedId != null) {
            val oldPosition = partners.indexOfFirst { it.id == oldSelectedId }
            if (oldPosition != -1) notifyItemChanged(oldPosition)
        }
        if (selectedPartnerId != null) {
            val newPosition = partners.indexOfFirst { it.id == selectedPartnerId }
            if (newPosition != -1) notifyItemChanged(newPosition)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartnerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_partner_avatar, parent, false)
        return PartnerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PartnerViewHolder, position: Int) {
        val partner = partners[position]

        val imageUrl = partner.profile?.profileImageUrl

        if (imageUrl != null && imageUrl.isNotBlank()) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .into(holder.partnerAvatar)
        } else {
            holder.partnerAvatar.setImageResource(R.drawable.default_profile)
        }

        if (partner.id == selectedPartnerId) {
            holder.plusIcon.setImageResource(R.drawable.ic_selected)
        } else {
            holder.plusIcon.setImageResource(R.drawable.ic_add_circle)
        }

        holder.itemView.setOnClickListener {
            onItemClick(partner)
        }
    }

    override fun getItemCount(): Int {
        return partners.size
    }

    class PartnerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val partnerAvatar: ShapeableImageView = itemView.findViewById(R.id.partnerAvatar)
        val plusIcon: ImageView = itemView.findViewById(R.id.plusIcon)
    }
}