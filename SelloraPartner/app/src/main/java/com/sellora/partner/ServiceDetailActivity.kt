package com.sellora.partner

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.sellora.partner.databinding.ActivityServiceDetailBinding
import com.sellora.partner.databinding.ItemDeliverableRowDetailBinding
import com.sellora.partner.repositories.ServicesRepository
import com.sellora.partner.viewmodels.ServiceDetailViewModel

class ServiceDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServiceDetailBinding
    private val servicesRepository = ServicesRepository()
    private val viewModel: ServiceDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServiceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val serviceId = intent.getStringExtra("service_id") ?: ""
        if (serviceId.isEmpty()) { finish(); return }

        observeViewModel()
        viewModel.fetchServiceDetails(serviceId)
    }

    private fun observeViewModel() {
        viewModel.service.observe(this) { service ->
            service?.let { setupUI(it) }
        }

        viewModel.deleteStatus.observe(this) { status ->
            when (status) {
                is ServiceDetailViewModel.DeleteStatus.Deleting -> {
                    // Show progress if needed
                }
                is ServiceDetailViewModel.DeleteStatus.Success -> {
                    Toast.makeText(this, "Service deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is ServiceDetailViewModel.DeleteStatus.Error -> {
                    Toast.makeText(this, "Error: ${status.message}", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
                finish()
            }
        }
    }

    private fun setupUI(service: FreelancerService) {
        val s = service

        binding.txtServiceName.text  = s.name
        binding.txtCategory.text     = s.category.ifEmpty { "General" }
        binding.txtDeliveryTime.text = s.deliveryTime.ifEmpty { "—" }
        binding.txtDescription.text  = s.description

        val basicPrice = formatPrice(s.basicPrice)
        val advPrice   = formatPrice(s.advPrice)
        val proPrice   = formatPrice(s.proPrice)

        binding.txtSelectedPrice.text = basicPrice

        // Description expand / collapse
        if (s.description.length < 120) {
            binding.txtMore.visibility = View.GONE
        } else {
            binding.txtDescription.maxLines = 3
            binding.txtMore.visibility = View.VISIBLE
            binding.txtMore.setOnClickListener {
                val isExpanded = binding.txtDescription.maxLines == Int.MAX_VALUE
                binding.txtDescription.maxLines = if (isExpanded) 3 else Int.MAX_VALUE
                binding.txtMore.text = if (isExpanded) "more" else "less"
            }
        }

        if (s.imageUri.isNotEmpty()) {
            binding.imgService.loadProfileImage(s.imageUri, R.drawable.img_placeholder_service)
        }

        // ✅ Deliverables — now uses deliverableTiers instead of deliverableChecks
        binding.layoutDeliverables.removeAllViews()
        s.deliverables.forEachIndexed { index, label ->
            val itemBinding = ItemDeliverableRowDetailBinding.inflate(
                layoutInflater, binding.layoutDeliverables, false
            )
            itemBinding.txtDeliverableName.text = label

            val tier = s.deliverableTiers.getOrElse(index) { "Basic" }
            // Basic = included in all, Adv = included in Adv+Pro, Pro = Pro only
            updateCheckUI(itemBinding.txtBasicCheck, tier == "Basic")
            updateCheckUI(itemBinding.txtAdvCheck,   tier == "Basic" || tier == "Adv")
            updateCheckUI(itemBinding.txtProCheck,   true) // Pro always includes everything

            binding.layoutDeliverables.addView(itemBinding.root)
        }

        binding.togglePlan.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            binding.txtSelectedPrice.text = when (checkedId) {
                R.id.btnBasic -> basicPrice
                R.id.btnAdv   -> advPrice
                R.id.btnPro   -> proPrice
                else          -> basicPrice
            }
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnEdit.setOnClickListener {
            startActivity(Intent(this, AddServiceActivity::class.java).apply {
                putExtra("edit_service_id", s.id)
            })
        }
        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Service")
                .setMessage("Are you sure?")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteService(s.id)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun formatPrice(p: String) = if (p.startsWith("₹")) p else "₹$p"

    private fun updateCheckUI(view: android.widget.TextView, isIncluded: Boolean) {
        view.text = if (isIncluded) "✓" else "✗"
        view.setTextColor(Color.parseColor(if (isIncluded) "#22C55E" else "#ADB5BD"))
    }
}