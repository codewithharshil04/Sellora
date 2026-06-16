package com.sellora.admin

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.sellora.admin.databinding.ActivityServicesBinding
import com.sellora.admin.databinding.ItemAdminServiceBinding

class ServicesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServicesBinding
    private val viewModel: ServicesViewModel by viewModels()
    private lateinit var adapter: ServiceAdapter
    private var allServices = listOf<AdminService>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.rvServices.layoutManager = LinearLayoutManager(this)
        binding.rvServices.itemAnimator = null
        adapter = ServiceAdapter(
            onToggle = { service, makeActive -> 
                viewModel.toggleService(service.id, makeActive) { success, error ->
                    if (success) {
                        Toast.makeText(this, if (makeActive) "Service activated" else "Service deactivated", Toast.LENGTH_SHORT).show()
                        viewModel.fetchServices()
                    } else {
                        Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDelete = { service -> confirmDelete(service) }
        )
        binding.rvServices.adapter = adapter

        setupObservers()

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { filter(s.toString()) }
            override fun afterTextChanged(s: Editable?) {}
        })

        NavigationUtils.setupBottomNavigation(this, binding.bottomNav, R.id.nav_services)

        binding.swipeRefresh.setColorSchemeResources(R.color.sellora_primary)
        binding.swipeRefresh.setOnRefreshListener { viewModel.fetchServices() }

        viewModel.fetchServices()
    }

    private fun setupObservers() {
        viewModel.services.observe(this) { services ->
            allServices = services
            binding.txtCount.text = "${allServices.size} services"
            binding.swipeRefresh.isRefreshing = false
            filter(binding.etSearch.text.toString())
        }

        viewModel.isLoading.observe(this) { isLoading ->
            if (!binding.swipeRefresh.isRefreshing) {
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Error: $it", Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun filter(query: String) {
        val filtered = if (query.isEmpty()) allServices
        else allServices.filter {
            it.serviceName.contains(query, true) || it.partnerName.contains(query, true) || it.category.contains(query, true)
        }
        adapter.submitList(filtered)
        binding.txtEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmDelete(service: AdminService) {
        AlertDialog.Builder(this)
            .setTitle("Delete Service")
            .setMessage("Delete \"${service.serviceName}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteService(service.id) { success, error ->
                    if (success) {
                        Toast.makeText(this, "Service deleted", Toast.LENGTH_SHORT).show()
                        viewModel.fetchServices()
                    } else {
                        Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

class ServiceAdapter(
    private val onToggle: (AdminService, Boolean) -> Unit,
    private val onDelete: (AdminService) -> Unit
) : ListAdapter<AdminService, ServiceAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AdminService>() {
            override fun areItemsTheSame(a: AdminService, b: AdminService) = a.id == b.id
            override fun areContentsTheSame(a: AdminService, b: AdminService) = a == b
        }
    }

    inner class VH(val binding: ItemAdminServiceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemAdminServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.binding

        b.txtServiceName.text = item.serviceName.ifEmpty { "—" }
        b.txtPartnerName.text = item.partnerName.ifEmpty { "Unknown Partner" }
        b.txtCategory.text = item.category
        b.txtPrice.text = if (item.minPrice.isNotEmpty() && item.maxPrice.isNotEmpty())
            "₹${item.minPrice}–₹${item.maxPrice}" else "Price on request"

        val statusColor = if (item.isActive) Color.parseColor("#10B981") else Color.parseColor("#EF4444")
        b.viewStatusDot.backgroundTintList = ColorStateList.valueOf(statusColor)

        if (item.imageUrl.isNotEmpty()) {
            b.imgService.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.img_placeholder_service)
                error(R.drawable.img_placeholder_service)
            }
        } else {
            b.imgService.setImageResource(R.drawable.img_placeholder_service)
        }

        MotionUtils.applyPressEffect(b.btnToggle)
        MotionUtils.applyPressEffect(b.btnDelete)
        b.btnToggle.text = if (item.isActive) "Deactivate" else "Activate"
        b.btnToggle.setOnClickListener { MotionUtils.pulseToggle(b.btnToggle); onToggle(item, !item.isActive) }
        b.btnDelete.setOnClickListener { onDelete(item) }

        MotionUtils.animateItemEntry(holder.itemView, position)
    }
}
