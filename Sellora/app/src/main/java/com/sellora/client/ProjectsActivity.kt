package com.sellora.client

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.sellora.client.databinding.ActivityProjectsBinding
import com.sellora.client.repositories.OrdersRepository
import java.text.SimpleDateFormat
import java.util.Locale

class ProjectsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProjectsBinding
    private lateinit var adapter: ProjectAdapter
    private var allProjects = mutableListOf<Project>()
    private var currentCheckedId: Int = R.id.btnNew
    private val ordersRepository = OrdersRepository()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.rvProjects.layoutManager = LinearLayoutManager(this)
        binding.rvProjects.itemAnimator = null
        adapter = ProjectAdapter(emptyList())
        binding.rvProjects.adapter = adapter

        binding.swipeRefresh.setColorSchemeResources(R.color.sellora_primary)
        binding.swipeRefresh.setOnRefreshListener {
            allProjects.clear()
            fetchClientOrders()
        }

        fetchClientOrders()

        // 3 tabs: New, Delivered, Cancelled
        binding.toggleStatus.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            currentCheckedId = checkedId
            applyFilter()
        }

        binding.bottomNav.selectedItemId = R.id.nav_projects
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_projects -> true
                R.id.nav_home -> { startActivity(Intent(this, HomeActivity::class.java)); finish(); false }
                R.id.nav_category -> {
                    val sheet = CategoryBottomSheet()
                    sheet.onCategorySelected = { category ->
                        val intent = Intent(this, HomeActivity::class.java)
                        intent.putExtra("selected_category", category)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                    }
                    sheet.show(supportFragmentManager, "CategoryBottomSheet")
                    true
                }
                R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); finish(); false }
                else -> false
            }
        }
    }

    private fun fetchClientOrders() {
        val uid = auth.currentUser?.uid ?: run {
            binding.txtEmpty.text = "Please log in to see your orders"
            binding.txtEmpty.visibility = View.VISIBLE
            return
        }

        ordersRepository.getClientOrders(uid) { documents, exception ->
            if (exception != null) {
                Log.e("ProjectsActivity", "Error fetching orders", exception)
                Toast.makeText(this, "Failed to load orders.", Toast.LENGTH_SHORT).show()
                binding.swipeRefresh.isRefreshing = false
                checkEmpty(emptyList())
                return@getClientOrders
            }

            allProjects.clear()
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            documents?.forEach { doc ->
                try {
                    val ts = doc.getTimestamp("createdAt")
                    val dateStr = if (ts != null) sdf.format(ts.toDate()) else "N/A"
                    val imageUrl = listOf(doc.getString("imageUrl"), doc.getString("imageUri"),
                        doc.getString("serviceImageUrl")).firstOrNull { !it.isNullOrBlank() && it != "null" } ?: ""
                    val photoUrl = listOf(doc.getString("freelancerPhotoUrl"),
                        doc.getString("partnerPhotoUrl")).firstOrNull { !it.isNullOrBlank() && it != "null" } ?: ""

                    allProjects.add(Project(
                        id = doc.id,
                        serviceName = doc.getString("serviceName") ?: "Service",
                        freelancerName = doc.getString("partnerName") ?: doc.getString("freelancerName") ?: "Freelancer",
                        date = dateStr,
                        price = doc.getString("price") ?: "₹0",
                        status = doc.getString("status") ?: "New",
                        serviceImageUrl = imageUrl,
                        freelancerPhotoUrl = photoUrl
                    ))
                } catch (e: Exception) {
                    Log.e("ProjectsActivity", "Error mapping order: ${e.message}")
                }
            }
            binding.swipeRefresh.isRefreshing = false
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filtered = when (currentCheckedId) {
            R.id.btnNew -> allProjects.filter { it.status == "New" || it.status == "Pending" }
            R.id.btnDelivered -> allProjects.filter { it.status == "Delivered" || it.status == "Completed" }
            R.id.btnCancelled -> allProjects.filter { it.status == "Cancelled" }
            else -> allProjects
        }
        adapter.updateList(filtered)
        checkEmpty(filtered)
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_projects
        if (allProjects.isEmpty()) fetchClientOrders()
        intent.getStringExtra("highlight_order_id")?.let { orderId ->
            adapter.highlightOrder(orderId)
            intent.removeExtra("highlight_order_id")
        }
    }

    private fun checkEmpty(list: List<Project>) {
        binding.txtEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }
}
