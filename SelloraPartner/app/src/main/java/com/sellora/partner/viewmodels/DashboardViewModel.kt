package com.sellora.partner.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sellora.partner.FreelancerService
import com.sellora.partner.repositories.OrdersRepository
import com.sellora.partner.repositories.ServicesRepository

class DashboardViewModel : ViewModel() {
    private val servicesRepository = ServicesRepository()
    private val ordersRepository = OrdersRepository()

    private val _services = MutableLiveData<List<FreelancerService>>()
    val services: LiveData<List<FreelancerService>> = _services

    private val _stats = MutableLiveData<DashboardStats>()
    val stats: LiveData<DashboardStats> = _stats

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    data class DashboardStats(
        val newCount: Int,
        val deliveredCount: Int,
        val cancelledCount: Int,
        val totalIncome: Double
    )

    fun fetchDashboardData(userId: String) {
        fetchServices(userId)
        fetchStats(userId)
    }

    fun fetchServices(userId: String) {
        _isLoading.value = true
        servicesRepository.getPartnerServices(userId)
            .addOnSuccessListener { documents ->
                val newList = mutableListOf<FreelancerService>()
                for (doc in documents) {
                    try {
                        newList.add(servicesRepository.mapDocument(doc.id, doc.data))
                    } catch (e: Exception) {
                        // Log error or handle
                    }
                }
                _services.value = newList
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                // Check for index-related errors
                val message = e.message ?: ""
                if (message.contains("FAILED_PRECONDITION") && message.contains("index")) {
                    _error.value = "Firestore index required. Check logcat for the link."
                } else {
                    _error.value = "Failed to load services: ${e.message}"
                }
                _isLoading.value = false
            }
    }

    fun fetchStats(userId: String) {
        ordersRepository.fetchOrdersByPartner(userId)
            .addOnSuccessListener { documents ->
                var new = 0; var delivered = 0; var cancelled = 0
                var totalIncome = 0.0

                for (doc in documents) {
                    val status = doc.getString("status") ?: ""
                    val project = ordersRepository.mapToProject(doc)

                    when (status) {
                        "New"         -> new++
                        "Delivered"   -> { delivered++; totalIncome += project.getNetEarning() }
                        "Cancelled"   -> cancelled++
                    }
                }
                _stats.value = DashboardStats(new, delivered, cancelled, totalIncome)
            }
    }
    
    fun clearError() {
        _error.value = null
    }
}
