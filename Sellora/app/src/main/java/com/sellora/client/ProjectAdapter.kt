package com.sellora.client

import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.sellora.client.databinding.ItemProjectBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ProjectAdapter(private var projects: List<Project>) :
    RecyclerView.Adapter<ProjectAdapter.ViewHolder>() {

    private var highlightedOrderId: String? = null

    inner class ViewHolder(val binding: ItemProjectBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProjectBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = projects[position]
        val binding = holder.binding
        val context = binding.root.context

        binding.txtServiceName.text = item.serviceName
        binding.txtFreelancerName.text = item.freelancerName
        binding.txtDate.text = context.getString(R.string.project_date_format, item.date)
        binding.txtPrice.text = if (item.price.startsWith("₹")) item.price else "₹${item.price}"
        binding.txtStatus.text = item.status
        binding.txtTimer.text = getElapsedLabel(item.date)

        // Apply highlighting if this order is highlighted
        if (item.id == highlightedOrderId) {
            binding.root.setBackgroundResource(R.color.highlight_order_bg)
        } else {
            binding.root.setBackgroundColor(Color.TRANSPARENT)
        }

        // Service Image
        val sUrl = item.serviceImageUrl?.trim()
        val isSValid = !sUrl.isNullOrEmpty() &&
                sUrl != "null" &&
                (sUrl.startsWith("http://") || sUrl.startsWith("https://"))

        if (isSValid) {
            binding.imgService.load(sUrl) {
                crossfade(true)
                placeholder(R.drawable.img_placeholder_service)
                error(R.drawable.img_placeholder_service)
                allowHardware(false)
            }
        } else {
            binding.imgService.setImageResource(R.drawable.img_placeholder_service)
        }

        // Profile Image
        val pUrl = item.freelancerPhotoUrl?.trim()
        val isPValid = !pUrl.isNullOrEmpty() &&
                pUrl != "null" &&
                (pUrl.startsWith("http://") || pUrl.startsWith("https://"))

        if (isPValid) {
            binding.imgProfile.load(pUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_profile_placeholder)
                error(R.drawable.ic_profile_placeholder)
                transformations(CircleCropTransformation())
                allowHardware(false)
            }
        } else {
            binding.imgProfile.setImageResource(R.drawable.ic_profile_placeholder)
        }

        // Status Color
        val colorRes = when (item.status) {
            "New", "Pending" -> R.color.status_pending
            "In Progress" -> R.color.status_in_progress
            "Delivered", "Completed" -> R.color.status_delivered
            "Cancelled" -> R.color.status_cancelled
            else -> R.color.status_default
        }

        val color = ContextCompat.getColor(context, colorRes)
        binding.txtStatus.background?.setTint(color)
        binding.txtStatus.setTextColor(
            if (item.status == "New" || item.status == "Pending")
                ContextCompat.getColor(context, R.color.sellora_text_dark)
            else Color.WHITE
        )

        binding.root.setOnClickListener {
            val intent = Intent(context, ProjectDetailActivity::class.java).apply {
                putExtra("project_id", item.id)
                putExtra("service_name", item.serviceName)
                putExtra("freelancer_name", item.freelancerName)
                putExtra("date", item.date)
                putExtra("price", binding.txtPrice.text.toString())
                putExtra("status", item.status)
                putExtra("timer", item.timer)
                putExtra("image_url", item.serviceImageUrl)
                putExtra("freelancer_photo_url", item.freelancerPhotoUrl)
            }
            context.startActivity(intent)
        }
    }

    private fun getElapsedLabel(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return dateStr
            val diffDays = ((System.currentTimeMillis() - date.time) / (1000 * 60 * 60 * 24)).toInt()

            when {
                diffDays == 0 -> "Today"
                diffDays == 1 -> "Yesterday"
                diffDays < 7 -> "$diffDays days ago"
                diffDays < 30 -> "${diffDays / 7}w ago"
                else -> "${diffDays / 30}mo ago"
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    override fun getItemCount() = projects.size

    fun updateList(newList: List<Project>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = projects.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                projects[oldItemPosition].id == newList[newItemPosition].id

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                projects[oldItemPosition] == newList[newItemPosition]
        })

        projects = newList
        diffResult.dispatchUpdatesTo(this)
    }

    fun highlightOrder(orderId: String) {
        highlightedOrderId = orderId
        notifyDataSetChanged()
    }
}
