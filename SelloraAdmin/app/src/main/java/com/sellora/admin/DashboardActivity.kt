package com.sellora.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import com.google.firebase.auth.FirebaseAuth
import com.sellora.admin.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: DashboardViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupObservers()
        viewModel.loadStats()
        applyEffects()
    }

    private fun setupObservers() {
        viewModel.userCount.observe(this) { count ->
            binding.txtUserCount.text = count.toString()
            MotionUtils.fadeIn(binding.txtUserCount)
        }
        viewModel.partnerCount.observe(this) { count ->
            binding.txtPartnerCount.text = count.toString()
            MotionUtils.fadeIn(binding.txtPartnerCount)
        }
        viewModel.serviceCount.observe(this) { count ->
            binding.txtServiceCount.text = count.toString()
            MotionUtils.fadeIn(binding.txtServiceCount)
        }
        viewModel.orderCount.observe(this) { count ->
            binding.txtOrderCount.text = count.toString()
            MotionUtils.fadeIn(binding.txtOrderCount)
        }
        viewModel.pendingCount.observe(this) { count ->
            binding.txtPendingCount.text = count.toString()
        }
        viewModel.deliveredCount.observe(this) { count ->
            binding.txtDeliveredCount.text = count.toString()
        }
        viewModel.cancelledCount.observe(this) { count ->
            binding.txtCancelledCount.text = count.toString()
        }
        viewModel.revenue.observe(this) { revenue ->
            binding.txtRevenue.text = revenue
            MotionUtils.fadeIn(binding.txtRevenue)
        }
    }

    private fun applyEffects() {
        MotionUtils.applyPressEffect(binding.cardUsers)
        MotionUtils.applyPressEffect(binding.cardPartners)
        MotionUtils.applyPressEffect(binding.cardServices)
        MotionUtils.applyPressEffect(binding.cardOrders)
    }

    override fun onResume() {
        super.onResume()
        NavigationUtils.setupBottomNavigation(this, binding.bottomNav, R.id.nav_dashboard)
    }

    private fun setupNavigation() {
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            NavigationUtils.navigateTo(this, LoginActivity::class.java)
            finishAffinity()
        }

        binding.cardUsers.setOnClickListener { NavigationUtils.navigateTo(this, UsersActivity::class.java) }
        binding.cardPartners.setOnClickListener { NavigationUtils.navigateTo(this, PartnersActivity::class.java) }
        binding.cardServices.setOnClickListener { NavigationUtils.navigateTo(this, ServicesActivity::class.java) }
        binding.cardOrders.setOnClickListener { NavigationUtils.navigateTo(this, OrdersActivity::class.java) }

        NavigationUtils.setupBottomNavigation(this, binding.bottomNav, R.id.nav_dashboard)
    }
}
