package com.sellora.admin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sellora.admin.databinding.ActivityOrdersBinding
import com.sellora.admin.databinding.ItemAdminOrderBinding

class OrdersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrdersBinding
    private val viewModel: OrdersViewModel by viewModels()
    private lateinit var adapter: OrderAdapter
    private var allOrders = listOf<AdminOrder>()
    private var currentStatus = "New"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.rvOrders.layoutManager = LinearLayoutManager(this)
        binding.rvOrders.itemAnimator = null
        adapter = OrderAdapter { order, newStatus -> 
            viewModel.updateOrderStatus(order.id, newStatus) { success, error ->
                if (success) {
                    Toast.makeText(this, "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
                    viewModel.fetchOrders(currentStatus)
                } else {
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.rvOrders.adapter = adapter

        setupObservers()

        // 3 status tabs: New, Delivered, Cancelled
        binding.toggleStatus.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            currentStatus = when (checkedId) {
                R.id.btnNew -> "New"
                R.id.btnDelivered -> "Delivered"
                R.id.btnCancelled -> "Cancelled"
                else -> "New"
            }
            viewModel.fetchOrders(currentStatus)
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { applyFilter() }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_orders -> true
                R.id.nav_dashboard -> { navigateTo(DashboardActivity::class.java); false }
                R.id.nav_services -> { navigateTo(ServicesActivity::class.java); false }
                R.id.nav_users -> { navigateTo(UsersActivity::class.java); false }
                else -> false
            }
        }

        binding.swipeRefresh.setColorSchemeResources(R.color.sellora_primary)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchOrders(currentStatus)
        }
    }

    private fun setupObservers() {
        viewModel.orders.observe(this) { orders ->
            allOrders = orders
            binding.txtCount.text = "${allOrders.size} $currentStatus orders"
            binding.swipeRefresh.isRefreshing = false
            applyFilter()
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Error: $it", Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun navigateTo(target: Class<*>) {
        val intent = Intent(this, target)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }

    private fun applyFilter() {
        val query = binding.etSearch.text.toString()
        val filtered = allOrders.filter { order ->
            query.isEmpty() ||
                order.serviceName.contains(query, true) ||
                order.clientName.contains(query, true) ||
                order.partnerName.contains(query, true)
        }
        adapter.submitList(filtered)
        binding.txtEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        NavigationUtils.setupBottomNavigation(this, binding.bottomNav, R.id.nav_orders)
        if (viewModel.orders.value.isNullOrEmpty()) viewModel.fetchOrders(currentStatus)
    }
}

class OrderAdapter(
    private val onStatusChange: (AdminOrder, String) -> Unit
) : ListAdapter<AdminOrder, OrderAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AdminOrder>() {
            override fun areItemsTheSame(a: AdminOrder, b: AdminOrder) = a.id == b.id
            override fun areContentsTheSame(a: AdminOrder, b: AdminOrder) = a == b
        }
    }

    inner class VH(val binding: ItemAdminOrderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemAdminOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.binding
        val context = b.root.context

        b.txtServiceName.text = item.serviceName.ifEmpty { "—" }
        b.txtClientName.text = "Client: ${item.clientName.ifEmpty { "—" }}"
        b.txtPartnerName.text = "Partner: ${item.partnerName.ifEmpty { "—" }}"
        b.txtPrice.text = if (item.price.startsWith("₹")) item.price else "₹${item.price}"
        b.txtDate.text = item.date
        b.txtPackage.text = item.packageType.ifEmpty { "—" }

        val (bgColor, textColor) = when (item.status) {
            "New", "Pending" -> context.getColor(R.color.status_pending_bg) to context.getColor(R.color.status_pending_text)
            "Delivered" -> context.getColor(R.color.status_delivered_bg) to context.getColor(R.color.status_delivered_text)
            "Cancelled" -> context.getColor(R.color.status_cancelled_bg) to context.getColor(R.color.status_cancelled_text)
            else -> context.getColor(R.color.status_default_bg) to context.getColor(R.color.status_default_text)
        }
        
        b.txtStatus.text = item.status
        b.txtStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(bgColor)
        b.txtStatus.setTextColor(textColor)

        // Action buttons based on current status
        when (item.status) {
            "New", "Pending" -> {
                b.btnAction1.visibility = View.VISIBLE
                b.btnAction2.visibility = View.VISIBLE
                b.btnAction1.text = "Deliver"
                b.btnAction2.text = "Cancel"
                MotionUtils.applyPressEffect(b.btnAction1)
                MotionUtils.applyPressEffect(b.btnAction2)
                b.btnAction1.setOnClickListener { onStatusChange(item, "Delivered") }
                b.btnAction2.setOnClickListener { onStatusChange(item, "Cancelled") }
            }
            "Delivered", "Cancelled" -> {
                b.btnAction1.visibility = View.VISIBLE
                b.btnAction2.visibility = View.GONE
                b.btnAction1.text = "Reopen"
                MotionUtils.applyPressEffect(b.btnAction1)
                b.btnAction1.setOnClickListener { onStatusChange(item, "New") }
            }
            else -> {
                b.btnAction1.visibility = View.GONE
                b.btnAction2.visibility = View.GONE
            }
        }

        MotionUtils.animateItemEntry(holder.itemView, position)
    }
}
