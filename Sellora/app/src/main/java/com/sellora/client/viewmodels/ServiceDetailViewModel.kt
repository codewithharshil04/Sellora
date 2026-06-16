package com.sellora.client.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.sellora.client.Service

class ServiceDetailViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _recommendedServices = MutableLiveData<List<Service>>()
    val recommendedServices: LiveData<List<Service>> = _recommendedServices

    fun fetchRecommendedServices(category: String, currentServiceId: String) {
        db.collection("services")
            .whereEqualTo("isActive", true)
            .whereEqualTo("category", category)
            .limit(6)
            .get()
            .addOnSuccessListener { snap ->
                val recommended = snap.documents
                    .filter { it.id != currentServiceId }
                    .mapNotNull { doc ->
                        try {
                            val sName = doc.getString("serviceName") ?: doc.getString("name") ?: return@mapNotNull null
                            val min = doc.get("minPrice")?.toString() ?: ""
                            val max = doc.get("maxPrice")?.toString() ?: ""
                            val price = if (min.isNotEmpty() && max.isNotEmpty()) "₹$min–₹$max" else "Price on request"
                            Service(
                                id = doc.id,
                                imageUrl = doc.getString("imageUrl") ?: doc.getString("imageUri") ?: "",
                                freelancerName = doc.getString("freelancerName") ?: doc.getString("partnerName") ?: "",
                                freelancerPhotoUrl = doc.getString("freelancerPhotoUrl") ?: "",
                                priceRange = price,
                                serviceName = sName,
                                partnerId = doc.getString("partnerId") ?: "",
                                category = doc.getString("category") ?: "",
                                description = doc.getString("description") ?: "",
                                basicPrice = doc.getString("basicPrice") ?: "₹0",
                                advPrice = doc.getString("advPrice") ?: "₹0",
                                proPrice = doc.getString("proPrice") ?: "₹0",
                                deliveryTime = doc.getString("deliveryTime") ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                _recommendedServices.value = recommended
            }
    }
}
