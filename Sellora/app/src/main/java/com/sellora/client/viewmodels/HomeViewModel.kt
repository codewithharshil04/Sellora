package com.sellora.client.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sellora.client.Service
import com.sellora.client.repositories.ServicesRepository
import com.sellora.client.repositories.UserRepository

class HomeViewModel : ViewModel() {

    private val servicesRepository = ServicesRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _services = MutableLiveData<List<Service>>()
    val services: LiveData<List<Service>> = _services

    private val _favoriteIds = MutableLiveData<Set<String>>()
    val favoriteIds: LiveData<Set<String>> = _favoriteIds

    private val _profileData = MutableLiveData<Map<String, Any>?>()
    val profileData: LiveData<Map<String, Any>?> = _profileData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var profileListener: ListenerRegistration? = null

    fun fetchServices() {
        _isLoading.value = true
        servicesRepository.fetchServices { documents, exception ->
            _isLoading.value = false
            if (exception != null) {
                _errorMessage.value = exception.message
                return@fetchServices
            }

            val list = documents?.mapNotNull { mapDocumentToService(it) } ?: emptyList()
            _services.value = list
        }
    }

    fun loadFavorites() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val list = (doc.get("favoriteServiceIds") as? List<*>)?.filterIsInstance<String>()
                _favoriteIds.value = list?.toSet() ?: emptySet()
            }
    }

    fun toggleFavorite(serviceId: String) {
        val userId = auth.currentUser?.uid ?: return
        val currentFavorites = _favoriteIds.value ?: emptySet()
        val isCurrentlyFavorite = serviceId in currentFavorites

        // Optimistic update
        val newFavorites = currentFavorites.toMutableSet()
        if (isCurrentlyFavorite) newFavorites.remove(serviceId) else newFavorites.add(serviceId)
        _favoriteIds.value = newFavorites

        val operation = if (isCurrentlyFavorite)
            FieldValue.arrayRemove(serviceId)
        else
            FieldValue.arrayUnion(serviceId)

        db.collection("users").document(userId)
            .update("favoriteServiceIds", operation)
            .addOnFailureListener {
                // Rollback on failure
                _favoriteIds.value = currentFavorites
                _errorMessage.value = "Failed to update favorite"
            }
    }

    fun startProfileObserver() {
        val uid = auth.currentUser?.uid ?: return
        profileListener = userRepository.observeUserProfile(uid) { data ->
            _profileData.value = data
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun mapDocumentToService(document: DocumentSnapshot): Service? {
        return try {
            val imageUrl = document.getString("imageUrl") ?: document.getString("imageUri") ?: document.getString("image") ?: ""
            val serviceName        = document.getString("serviceName") ?: document.getString("name") ?: "Service"
            val partnerId          = document.getString("partnerId") ?: ""
            val freelancerName     = document.getString("freelancerName") ?: document.getString("partnerName") ?: "Freelancer"
            val freelancerPhotoUrl = document.getString("freelancerPhotoUrl") ?: document.getString("partnerPhotoUrl") ?: ""

            val minPrice  = document.get("minPrice")?.toString()
            val maxPrice  = document.get("maxPrice")?.toString()
            val priceRange = if (minPrice != null && maxPrice != null) "₹$minPrice–₹$maxPrice"
            else document.getString("priceRange") ?: "Price on request"

            val deliverables = (document.get("deliverables") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val deliverableTiers = (document.get("deliverableTiers") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val deliveryTime = document.getString("deliveryTime") ?: ""

            Service(
                id                 = document.id,
                imageUrl           = imageUrl,
                freelancerName     = freelancerName,
                freelancerPhotoUrl = freelancerPhotoUrl,
                priceRange         = priceRange,
                serviceName        = serviceName,
                partnerId          = partnerId,
                category           = document.getString("category") ?: "Other",
                description        = document.getString("description") ?: "",
                basicPrice         = document.getString("basicPrice") ?: "₹0",
                advPrice           = document.getString("advPrice")   ?: "₹0",
                proPrice           = document.getString("proPrice")   ?: "₹0",
                deliverables       = deliverables,
                deliverableTiers   = deliverableTiers,
                deliveryTime       = deliveryTime
            )
        } catch (e: Exception) {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        profileListener?.remove()
    }
}
