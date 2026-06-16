package com.sellora.admin

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sellora.admin.databinding.ActivityPartnersBinding
import com.sellora.admin.databinding.ItemAdminPartnerBinding

class PartnersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPartnersBinding
    private val viewModel: PartnersViewModel by viewModels()
    private lateinit var adapter: PartnerAdapter
    private var allPartners = listOf<AdminPartner>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPartnersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.rvPartners.layoutManager = LinearLayoutManager(this)
        binding.rvPartners.itemAnimator = null
        adapter = PartnerAdapter { partner, makeActive ->
            viewModel.togglePartnerStatus(partner.id, makeActive) { success, error ->
                if (success) {
                    Toast.makeText(this, if (makeActive) "Partner activated" else "Partner deactivated", Toast.LENGTH_SHORT).show()
                    viewModel.fetchPartners()
                } else {
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.rvPartners.adapter = adapter

        setupObservers()

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { filter(s.toString()) }
            override fun afterTextChanged(s: Editable?) {}
        })

        NavigationUtils.setupBottomNavigation(this, binding.bottomNav, R.id.nav_users)

        binding.swipeRefresh.setColorSchemeResources(R.color.sellora_primary)
        binding.swipeRefresh.setOnRefreshListener { viewModel.fetchPartners() }

        viewModel.fetchPartners()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_users
    }

    private fun setupObservers() {
        viewModel.partners.observe(this) { partners ->
            allPartners = partners
            binding.txtCount.text = "${allPartners.size} partners"
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
        val filtered = if (query.isEmpty()) allPartners
        else allPartners.filter { it.name.contains(query, true) || it.email.contains(query, true) }
        adapter.submitList(filtered)
        binding.txtEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }
}

class PartnerAdapter(
    private val onToggle: (AdminPartner, Boolean) -> Unit
) : ListAdapter<AdminPartner, PartnerAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AdminPartner>() {
            override fun areItemsTheSame(a: AdminPartner, b: AdminPartner) = a.id == b.id
            override fun areContentsTheSame(a: AdminPartner, b: AdminPartner) = a == b
        }
    }

    inner class VH(val binding: ItemAdminPartnerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemAdminPartnerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.binding

        b.txtName.text = item.name.ifEmpty { "—" }
        b.txtEmail.text = item.email.ifEmpty { "—" }
        b.txtPhone.text = item.phone.ifEmpty { "—" }
        b.txtAvatar.text = item.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        val isActive = item.isActive
        val dot = b.chipStatus.findViewById<View>(R.id.dotStatus)
        val label = b.chipStatus.findViewById<TextView>(R.id.txtStatusLabel)
        dot.setBackgroundColor(if (isActive) Color.parseColor("#22C55E") else Color.parseColor("#EF4444"))
        label.text = if (isActive) "Active" else "Inactive"
        label.setTextColor(if (isActive) Color.parseColor("#065F46") else Color.parseColor("#7F1D1D"))

        MotionUtils.applyPressEffect(b.btnToggle)
        b.btnToggle.text = if (isActive) "Deactivate" else "Activate"
        b.btnToggle.setOnClickListener {
            MotionUtils.pulseToggle(b.btnToggle)
            onToggle(item, !isActive)
        }

        MotionUtils.animateItemEntry(holder.itemView, position)
    }
}
