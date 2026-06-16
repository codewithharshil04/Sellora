package com.sellora.client

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sellora.client.databinding.ItemServiceSelloraBinding
import com.google.gson.Gson

class ServiceAdapter(
    private var favoriteIds: Set<String> = emptySet(),
    private val onFavoriteClick: (String) -> Unit
) : ListAdapter<Service, ServiceAdapter.ServiceViewHolder>(ServiceDiffCallback()) {

    private val gson = Gson()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val binding = ItemServiceSelloraBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ServiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.any { it == "PAYLOAD_FAVORITE" }) {
            holder.updateHeart(getItem(position).id in favoriteIds)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun updateFavorites(newIds: Set<String>) {
        val oldIds = favoriteIds
        favoriteIds = newIds
        
        // Find items that actually changed their favorite status and notify them
        for (i in 0 until itemCount) {
            val id = getItem(i).id
            if ((id in oldIds) != (id in newIds)) {
                notifyItemChanged(i, "PAYLOAD_FAVORITE")
            }
        }
    }

    inner class ServiceViewHolder(private val binding: ItemServiceSelloraBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Apply touch effect once per viewholder
            MotionUtils.applyPressEffect(binding.root)
            MotionUtils.applyPressEffect(binding.btnFavorite)
        }

        fun bind(service: Service) {
            binding.txtFreelancerName.text = service.freelancerName
            binding.txtPriceRange.text     = service.priceRange
            binding.txtServiceName.text    = service.serviceName
            binding.txtCategoryTag.text    = service.category

            binding.imgService.loadImage(service.imageUrl, R.drawable.img_placeholder_service)
            binding.imgProfile.loadImage(service.freelancerPhotoUrl, R.drawable.ic_profile_placeholder)

            updateHeart(service.id in favoriteIds)

            binding.btnFavorite.setOnClickListener {
                onFavoriteClick(service.id)
            }

            binding.root.setOnClickListener {
                val context = binding.root.context
                context.startActivity(
                    Intent(context, ServiceDetailActivity::class.java).apply {
                        putExtra("service_json", Gson().toJson(service))
                    }
                )
            }
        }

        fun updateHeart(isSaved: Boolean) {
            val color = if (isSaved) R.color.heart_active else R.color.heart_inactive
            binding.imgHeart.setColorFilter(ContextCompat.getColor(binding.root.context, color))
        }
    }

    class ServiceDiffCallback : DiffUtil.ItemCallback<Service>() {
        override fun areItemsTheSame(oldItem: Service, newItem: Service): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Service, newItem: Service): Boolean = oldItem == newItem
    }
}
