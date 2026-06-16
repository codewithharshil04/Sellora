package com.sellora.partner

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.sellora.partner.databinding.ItemProjectBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ProjectAdapter(
    private var projects: List<Project>
) : RecyclerView.Adapter<ProjectAdapter.ViewHolder>() {

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

        binding.txtServiceName.text = item.serviceName
        binding.txtClientName.text  = item.freelancerName
        binding.txtDate.text        = formatDate(item.date)
        binding.txtPrice.text       = if (item.price.startsWith("₹")) item.price else "₹${item.price}"
        binding.txtStatus.text      = item.status
        binding.txtTimer.text       = getElapsedLabel(item.date)

        // Apply highlighting if this order is highlighted
        if (item.id == highlightedOrderId) {
            binding.root.setBackgroundColor(Color.parseColor("#FFF3E0")) // Light orange highlight
        } else {
            binding.root.setBackgroundColor(Color.TRANSPARENT)
        }

        val color = when (item.status) {
            "New"         -> "#F59E0B"
            "In Progress" -> "#014751"
            "Delivered"   -> "#10B981"
            "Cancelled"   -> "#EF4444"
            else          -> "#8E8E8E"
        }

        binding.txtStatus.background?.setTint(Color.parseColor(color))
        binding.txtStatus.setTextColor(
            if (item.status == "New") Color.parseColor("#1A1A1A") else Color.WHITE
        )

        // =========================
        // 🔥 SERVICE IMAGE
        // =========================
        binding.imgService.loadProfileImage(item.serviceImageUrl, R.drawable.img_placeholder_service)

        // =========================
        // 🔥 PROFILE IMAGE
        // =========================
        binding.imgProfile.loadProfileImage(item.clientPhotoUrl, R.drawable.ic_profile_placeholder)

        // =========================
        // 🚀 CLICK
        // =========================
        binding.root.setOnClickListener {
            val intent = Intent(binding.root.context, ProjectDetailActivity::class.java)
            intent.putExtra("order_id", item.id)
            intent.putExtra("service_name", item.serviceName)
            intent.putExtra("client_name", item.freelancerName)
            intent.putExtra("date", item.date)
            intent.putExtra("price", item.price)
            intent.putExtra("status", item.status)
            intent.putExtra("timer", item.timer)
            intent.putExtra("image_url", item.serviceImageUrl)
            
            // Pass requirements instantly to avoid UI flicker
            intent.putExtra("requirements", item.requirements)
            intent.putExtra("requirementFileUrl", item.requirementFileUrl)
            intent.putExtra("deliveryFileUrl", item.deliveryFileUrl)
            intent.putExtra("delivery_days", item.deliveryDays)
            intent.putExtra("created_at", item.createdAt)

            binding.root.context.startActivity(intent)
        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            if (date != null) outputFormat.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun getElapsedLabel(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return dateStr
            val diffDays = ((System.currentTimeMillis() - date.time) / (1000 * 60 * 60 * 24)).toInt()

            when {
                diffDays == 0 -> "Today"
                diffDays == 1 -> "Yesterday"
                diffDays < 7  -> "$diffDays days ago"
                diffDays < 30 -> "${diffDays / 7}w ago"
                else          -> "${diffDays / 30}mo ago"
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    override fun getItemCount() = projects.size

    fun updateList(newList: List<Project>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = projects.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(old: Int, new: Int) =
                projects[old].id == newList[new].id
            override fun areContentsTheSame(old: Int, new: Int) =
                projects[old] == newList[new]
        })

        projects = newList
        diff.dispatchUpdatesTo(this)
    }

    fun highlightOrder(orderId: String) {
        highlightedOrderId = orderId
        notifyDataSetChanged()
    }
}
