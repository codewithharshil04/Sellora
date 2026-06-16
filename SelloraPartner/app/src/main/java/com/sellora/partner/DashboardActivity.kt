package com.sellora.partner

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import coil.load
import coil.transform.CircleCropTransformation
import com.sellora.partner.databinding.ActivityDashboardBinding
import com.sellora.partner.viewmodels.DashboardViewModel

class DashboardActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var serviceAdapter: ServiceCardAdapter
    private val viewModel: DashboardViewModel by viewModels()

    override val drawerLayout: DrawerLayout get() = binding.main
    override val closeDrawerButton: View get() = binding.btnCloseDrawer
    override val navDashboard: View get() = binding.navDashboard
    override val navAddService: View get() = binding.navAddService
    override val navProjects: View get() = binding.navProjects
    override val navProfile: View get() = binding.navProfile
    override val navLogout: View get() = binding.navLogout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (auth.currentUser == null) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        val uid = auth.currentUser?.uid
        if (uid != null) {
            (application as SelloraPartnerApplication).startOrderListener(uid)
        }

        setupDrawer()
        tintDrawerIcons(
            binding.navDashboard,
            binding.navAddService,
            binding.navProjects,
            binding.navProfile,
            binding.navLogout
        )
        initServiceList()
        setupClickListeners()
        setupSwipeRefresh()
        observeViewModel()

        loadUserProfile()

        val userId = auth.currentUser?.uid
        if (userId != null) {
            viewModel.fetchDashboardData(userId)
        }
    }

    private fun observeViewModel() {
        viewModel.services.observe(this) { services ->
            serviceAdapter.submitList(services)
            binding.txtNoServices.visibility = if (services.isEmpty()) View.VISIBLE else View.GONE
            binding.swipeRefresh.isRefreshing = false
            LoadingUtils.hideLoading(binding.rvServices)
        }

        viewModel.stats.observe(this) { stats ->
            binding.txtNewCount.text        = stats.newCount.toString()
            binding.txtDeliveredCount.text  = stats.deliveredCount.toString()
            binding.txtCancelledCount.text  = stats.cancelledCount.toString()
            binding.txtTotalIncome.text     = "₹%.2f".format(stats.totalIncome)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading && !binding.swipeRefresh.isRefreshing) {
                LoadingUtils.showLoading(binding.rvServices, "Loading Services...")
            } else if (!isLoading) {
                LoadingUtils.hideLoading(binding.rvServices)
                binding.swipeRefresh.isRefreshing = false
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupClickListeners() {
        binding.cardNew.setOnClickListener {
            openProjectsWithFilter(R.id.btnNew)
        }
        binding.cardDelivered.setOnClickListener {
            openProjectsWithFilter(R.id.btnDelivered)
        }
        binding.cardCancelled.setOnClickListener {
            openProjectsWithFilter(R.id.btnCancelled)
        }
        binding.cardIncome.setOnClickListener {
            openProjectsWithFilter(R.id.btnDelivered)
        }

    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.sellora_primary)
        binding.swipeRefresh.setOnRefreshListener {
            val userId = auth.currentUser?.uid ?: return@setOnRefreshListener
            viewModel.fetchDashboardData(userId)
        }
    }





    private fun loadUserProfile() {
        // Profile photo removed from dashboard header
    }

    private fun openProjectsWithFilter(filterId: Int) {
        val intent = Intent(this, ProjectsActivity::class.java).apply {
            putExtra("SELECT_TAB_ID", filterId)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        val userId = auth.currentUser?.uid
        if (userId != null) {
            viewModel.fetchDashboardData(userId)
        }
        loadUserProfile()
    }

    private fun setupDrawer() {
        val prefs = getSharedPreferences("sellora_partner_auth", MODE_PRIVATE)
        val name  = prefs.getString("user_name", "Freelancer") ?: "Freelancer"
        binding.txtWelcome.text = getString(R.string.welcome_format, name)

        binding.btnMenu.setOnClickListener { binding.main.openDrawer(GravityCompat.END) }
        setupBaseDrawer()
    }

    private fun initServiceList() {
        binding.rvServices.layoutManager = LinearLayoutManager(this)
        
        // Disable change animations to stop flickering
        (binding.rvServices.itemAnimator as? SimpleItemAnimator)?.apply {
            supportsChangeAnimations = false
        }
        
        serviceAdapter = ServiceCardAdapter { service ->
            startActivity(Intent(this, AddServiceActivity::class.java).apply {
                putExtra("edit_service_id", service.id)
            })
        }
        binding.rvServices.adapter = serviceAdapter
        
        // Configure smooth scroll physics
        MotionUtils.configureSmoothScroll(binding.rvServices)

        binding.btnAddService.setOnClickListener {
            LoadingUtils.scalePress(binding.btnAddService)
            startActivity(Intent(this, AddServiceActivity::class.java))
            LoadingUtils.scaleRelease(binding.btnAddService)
        }
    }
}
