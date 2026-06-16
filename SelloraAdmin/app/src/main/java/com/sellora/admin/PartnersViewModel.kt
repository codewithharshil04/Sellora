package com.sellora.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class PartnersViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _partners = MutableLiveData<List<AdminPartner>>()
    val partners: LiveData<List<AdminPartner>> = _partners

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchPartners() {
        _isLoading.value = true
        db.collection("partners").get()
            .addOnSuccessListener { snap ->
                _isLoading.value = false
                val fetchedPartners = snap.documents.map { doc ->
                    AdminPartner(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        phone = doc.getString("phone") ?: "",
                        isActive = doc.getBoolean("isActive") ?: true
                    )
                }
                _partners.value = fetchedPartners
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _error.value = e.message ?: "Unknown error"
            }
    }

    fun togglePartnerStatus(partnerId: String, makeActive: Boolean, onComplete: (Boolean, String?) -> Unit) {
        db.collection("partners").document(partnerId)
            .update("isActive", makeActive)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, e.message)
            }
    }

    fun clearError() { _error.value = null }
}
