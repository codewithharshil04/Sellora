package com.sellora.partner

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.sellora.partner.databinding.ActivityProjectsBinding
import com.sellora.partner.viewmodels.ProjectsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class ProjectsActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityProjectsBinding
    private lateinit var adapter: ProjectAdapter
    private val viewModel: ProjectsViewModel by viewModels()

    private var currentCheckedId: Int = R.id.btnNew
    private var projectList = mutableListOf<Project>()

    override val drawerLayout: DrawerLayout get() = binding.main
    override val closeDrawerButton: View get() = binding.btnCloseDrawer
    override val navDashboard: View get() = binding.navDashboard
    override val navAddService: View get() = binding.navAddService
    override val navProjects: View get() = binding.navProjects
    override val navProfile: View get() = binding.navProfile
    override val navLogout: View get() = binding.navLogout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDrawer()
        setupProjects()
        setupSearch()
        observeViewModel()

        // Handle initial filter from intent
        val initialFilter = intent.getIntExtra("SELECT_TAB_ID", R.id.btnNew)
        binding.toggleStatus.check(initialFilter)
        currentCheckedId = initialFilter
    }

    override fun onResume() {
        super.onResume()
        binding.navProjects.isSelected = true 
        // Always refresh to show latest status changes from detail screen
        val userId = auth.currentUser?.uid
        if (userId != null) {
            viewModel.fetchProjects(userId)
        }
        handleNotificationHighlighting()
    }

    private fun observeViewModel() {
        viewModel.projects.observe(this) { projects ->
            projectList.clear()
            projectList.addAll(projects)
            filterByTab(currentCheckedId)
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.isRefreshing.observe(this) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupDrawer() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnMenuProjects.setOnClickListener {
            binding.main.openDrawer(GravityCompat.END)
        }
        setupBaseDrawer()
    }

    private fun setupProjects() {
        binding.rvProjects.layoutManager = LinearLayoutManager(this)
        adapter = ProjectAdapter(emptyList())
        binding.rvProjects.adapter = adapter

        binding.toggleStatus.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            currentCheckedId = checkedId
            filterByTab(checkedId)
        }

        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.sellora_white)
        binding.swipeRefresh.setColorSchemeResources(R.color.sellora_primary)
        binding.swipeRefresh.setOnRefreshListener {
            val userId = auth.currentUser?.uid ?: return@setOnRefreshListener
            viewModel.fetchProjects(userId)
        }
    }

    private fun setupSearch() {
        binding.edtSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterByTab(currentCheckedId)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun filterByTab(checkedId: Int) {
        val statusList = when (checkedId) {
            R.id.btnNew       -> listOf("New", "Pending", "In Progress")
            R.id.btnDelivered -> listOf("Delivered")
            R.id.btnCancelled -> listOf("Cancelled")
            else              -> listOf("New")
        }
        
        val query = binding.edtSearch.text.toString().trim()
        val queryTerms = if (query.isEmpty()) emptyList() else query.split("\\s+".toRegex()).map { it.lowercase() }

        val filtered = projectList.filter { project ->
            val matchesStatus = statusList.contains(project.status)
            if (!matchesStatus) return@filter false

            if (queryTerms.isEmpty()) return@filter true

            // Match if ALL terms of the query are found in ANY of the searchable fields
            queryTerms.all { term ->
                project.serviceName.lowercase().contains(term) ||
                project.freelancerName.lowercase().contains(term) ||
                project.price.lowercase().contains(term) ||
                project.date.lowercase().contains(term)
            }
        }

        adapter.updateList(filtered)
        checkEmpty(filtered)
    }

    private fun checkEmpty(list: List<Project>) {
        binding.txtEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun handleNotificationHighlighting() {
        intent.getStringExtra("highlight_order_id")?.let { orderId ->
            highlightOrder(orderId)
            intent.removeExtra("highlight_order_id")
        }
    }

    private fun highlightOrder(orderId: String) {
        adapter.highlightOrder(orderId)
    }
}
