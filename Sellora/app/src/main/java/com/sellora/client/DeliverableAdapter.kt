package com.sellora.client

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sellora.client.databinding.ItemDeliverableRowBinding

class DeliverableAdapter(
    private val deliverables: List<Deliverable>,
    private var activePlan: String = "Basic"
) : RecyclerView.Adapter<DeliverableAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDeliverableRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeliverableRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item     = deliverables[position]
        val binding  = holder.binding
        val included = item.isIncludedIn(activePlan)

        binding.txtDeliverable.text  = item.name
        binding.txtDeliverable.alpha = if (included) 1f else 0.38f

        if (included) {
            binding.vDot.setBackgroundResource(R.drawable.bg_green_circle)
            binding.txtTierBadge.visibility = View.GONE
        } else {
            binding.vDot.setBackgroundResource(R.drawable.bg_circle_outline)
            binding.txtTierBadge.visibility = View.VISIBLE
            binding.txtTierBadge.text       = item.tier
        }
    }

    override fun getItemCount() = deliverables.size

    fun setActivePlan(plan: String) {
        activePlan = plan
        notifyDataSetChanged()
    }
}
