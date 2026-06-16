package com.sellora.partner.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sellora.partner.FreelancerService
import com.sellora.partner.repositories.ServicesRepository

class ServiceDetailViewModel : ViewModel() {
    private val servicesRepository = ServicesRepository()

    private val _service = MutableLiveData<FreelancerService?>()
    val service: LiveData<FreelancerService?> = _service

    private val _deleteStatus = MutableLiveData<DeleteStatus>()
    val deleteStatus: LiveData<DeleteStatus> = _deleteStatus

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    sealed class DeleteStatus {
        object Idle : DeleteStatus()
        object Deleting : DeleteStatus()
        object Success : DeleteStatus()
        data class Error(val message: String) : DeleteStatus()
    }

    fun fetchServiceDetails(serviceId: String) {
        servicesRepository.getServiceById(serviceId)
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    _service.value = servicesRepository.mapDocument(document.id, document.data ?: emptyMap())
                } else {
                    _error.value = "Service not found"
                }
            }
            .addOnFailureListener { e ->
                _error.value = "Error fetching service: ${e.message}"
            }
    }

    fun deleteService(serviceId: String) {
        _deleteStatus.value = DeleteStatus.Deleting
        servicesRepository.deleteService(serviceId)
            .addOnSuccessListener {
                _deleteStatus.value = DeleteStatus.Success
            }
            .addOnFailureListener { e ->
                _deleteStatus.value = DeleteStatus.Error(e.message ?: "Deletion failed")
            }
    }
    
    fun clearError() {
        _error.value = null
    }
}
