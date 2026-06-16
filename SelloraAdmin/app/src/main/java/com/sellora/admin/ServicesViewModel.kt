package com.sellora.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class ServicesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _services = MutableLiveData<List<AdminService>>()
    val services: LiveData<List<AdminService>> = _services

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchServices() {
        _isLoading.value = true
        db.collection("services").get()
            .addOnSuccessListener { snap ->
                _isLoading.value = false
                val fetchedServices = snap.documents.map { doc ->
                    AdminService(
                        id = doc.id,
                        serviceName = doc.getString("serviceName") ?: doc.getString("name") ?: "",
                        partnerName = doc.getString("partnerName") ?: doc.getString("freelancerName") ?: "",
                        category = doc.getString("category") ?: "",
                        minPrice = doc.get("minPrice")?.toString() ?: "",
                        maxPrice = doc.get("maxPrice")?.toString() ?: "",
                        isActive = doc.getBoolean("isActive") ?: true,
                        imageUrl = doc.getString("imageUrl") ?: doc.getString("imageUri") ?: ""
                    )
                }
                _services.value = fetchedServices
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _error.value = e.message ?: "Unknown error"
            }
    }

    fun toggleService(serviceId: String, makeActive: Boolean, onComplete: (Boolean, String?) -> Unit) {
        db.collection("services").document(serviceId).update("isActive", makeActive)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { e -> onComplete(false, e.message) }
    }

    fun deleteService(serviceId: String, onComplete: (Boolean, String?) -> Unit) {
        db.collection("services").document(serviceId).delete()
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { e -> onComplete(false, e.message) }
    }

    fun clearError() { _error.value = null }
}
