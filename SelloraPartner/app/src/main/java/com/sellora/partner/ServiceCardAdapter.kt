package com.sellora.partner

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sellora.partner.databinding.ServiceCardBinding

class ServiceCardAdapter(
    private val onEdit: (FreelancerService) -> Unit = {}
) : ListAdapter<FreelancerService, ServiceCardAdapter.ViewHolder>(ServiceDiffCallback()) {

    inner class ViewHolder(val binding: ServiceCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ServiceCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val binding = holder.binding

        binding.txtServiceName.text = item.name
        binding.txtCategoryName.text = item.category
        binding.txtPriceRange.text  = if (item.minPrice.isNotEmpty() && item.maxPrice.isNotEmpty())
            "₹${item.minPrice} – ₹${item.maxPrice}" else ""

        // Use the centralized ImageUtils loader
        binding.imgService.loadProfileImage(item.imageUri, R.drawable.img_placeholder_service)

        // Apply subtle press effect to card
        MotionUtils.applyPressEffect(binding.root)

        binding.root.setOnClickListener {
            val intent = Intent(binding.root.context, ServiceDetailActivity::class.java)
            intent.putExtra("service_id", item.id)
            binding.root.context.startActivity(intent)
        }
        
        // If there's an edit action needed, it can be triggered here or via a specific button
        // binding.btnEdit.setOnClickListener { onEdit(item) }
    }

    class ServiceDiffCallback : DiffUtil.ItemCallback<FreelancerService>() {
        override fun areItemsTheSame(oldItem: FreelancerService, newItem: FreelancerService): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FreelancerService, newItem: FreelancerService): Boolean {
            return oldItem == newItem
        }
    }
}
