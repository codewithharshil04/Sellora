package com.sellora.client

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.firebase.auth.FirebaseAuth
import com.sellora.client.databinding.ActivityHomeBinding
import com.sellora.client.viewmodels.HomeViewModel

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: ServiceAdapter
    private var currentCategory: String = "All"
    private var serviceList: List<Service> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val authPrefs = getSharedPreferences("sellora_client_auth", MODE_PRIVATE)
        updateGreeting(authPrefs.getString("user_name", "") ?: "")

        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Start OrderStatusListener when user is logged in
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            (application as SelloraApplication).startOrderListener(uid)
        }

        intent.getStringExtra("selected_category")?.let { currentCategory = it }

        viewModel.fetchServices()
        viewModel.loadFavorites()
        viewModel.startProfileObserver()
    }

    private fun setupRecyclerView() {
        binding.rvListings.layoutManager = LinearLayoutManager(this)
        binding.rvListings.setHasFixedSize(true)
        (binding.rvListings.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        adapter = ServiceAdapter(emptySet()) { serviceId ->
            viewModel.toggleFavorite(serviceId)
        }
        binding.rvListings.adapter = adapter

        MotionUtils.configureSmoothScroll(binding.rvListings)
    }

    private fun setupObservers() {
        viewModel.services.observe(this) { services ->
            serviceList = services
            filterServices(binding.etSearch.text.toString(), currentCategory)
        }

        viewModel.favoriteIds.observe(this) { favorites ->
            adapter.updateFavorites(favorites.toMutableSet())
        }

        viewModel.profileData.observe(this) { data ->
            if (data != null) {
                val name = data["name"] as? String ?: ""
                val photoUrl = data["photoUrl"] as? String
                updateGreeting(name)
                binding.imgProfileThumb.loadImage(photoUrl, R.drawable.ic_profile_placeholder)

                getSharedPreferences("sellora_client_auth", MODE_PRIVATE)
                    .edit().apply {
                        putString("user_name", name)
                        putString("profile_pic_url", photoUrl)
                        apply()
                    }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                LoadingUtils.showLoading(binding.rvListings, "Loading Services...")
            } else {
                LoadingUtils.hideLoading(binding.rvListings)
                binding.swipeRefreshHome.isRefreshing = false
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupListeners() {
        binding.swipeRefreshHome.setColorSchemeResources(R.color.sellora_primary)
        binding.swipeRefreshHome.setOnRefreshListener {
            viewModel.fetchServices()
        }

        binding.imgProfileThumb.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        binding.bottomNav.selectedItemId = R.id.nav_home
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (currentCategory != "All" && binding.bottomNav.selectedItemId != R.id.nav_home) {
                        currentCategory = "All"
                        filterServices(binding.etSearch.text.toString(), "All")
                    }
                    true
                }
                R.id.nav_category -> {
                    val sheet = CategoryBottomSheet()
                    sheet.onCategorySelected = { category ->
                        currentCategory = category
                        filterServices(binding.etSearch.text.toString(), category)
                    }
                    sheet.show(supportFragmentManager, "CategoryBottomSheet")
                    false // Return false so 'Category' doesn't stay selected
                }
                R.id.nav_projects -> {
                    startActivity(Intent(this, ProjectsActivity::class.java))
                    false
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    false
                }
                else -> false
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterServices(s.toString(), currentCategory)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateGreeting(name: String) {
        binding.txtGreeting.text = if (name.isNotEmpty()) "Hey, $name 👋" else "Welcome 👋"
    }

    private fun filterServices(query: String, category: String) {
        val filtered = serviceList.filter {
            val matchesQuery    = it.serviceName.contains(query, ignoreCase = true) ||
                    it.freelancerName.contains(query, ignoreCase = true)
            val matchesCategory = category == "All" || it.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
        adapter.submitList(filtered)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("selected_category")?.let {
            currentCategory = it
            if (binding.bottomNav.selectedItemId != R.id.nav_home) {
                binding.bottomNav.selectedItemId = R.id.nav_home
            }
            filterServices(binding.etSearch.text.toString(), it)
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.services.value.isNullOrEmpty()) viewModel.fetchServices()
    }
}
