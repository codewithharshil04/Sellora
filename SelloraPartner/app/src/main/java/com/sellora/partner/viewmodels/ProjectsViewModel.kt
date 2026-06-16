package com.sellora.partner.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sellora.partner.Project
import com.sellora.partner.repositories.OrdersRepository

class ProjectsViewModel : ViewModel() {
    private val ordersRepository = OrdersRepository()

    private val _projects = MutableLiveData<List<Project>>()
    val projects: LiveData<List<Project>> = _projects

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchProjects(userId: String) {
        _isRefreshing.value = true
        ordersRepository.fetchOrdersByPartner(userId)
            .addOnSuccessListener { documents ->
                val projectList = mutableListOf<Project>()
                for (doc in documents) {
                    projectList.add(ordersRepository.mapToProject(doc))
                }
                
                // Sort by date (newest first)
                projectList.sortByDescending { it.date }
                
                _projects.value = projectList
                _isRefreshing.value = false
            }
            .addOnFailureListener { e ->
                _error.value = "Failed to load projects: ${e.message}"
                _isRefreshing.value = false
            }
    }
    
    fun clearError() {
        _error.value = null
    }
}
