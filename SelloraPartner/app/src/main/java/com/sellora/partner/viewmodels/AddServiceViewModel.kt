package com.sellora.partner.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sellora.partner.FreelancerService
import com.sellora.partner.repositories.MediaRepository
import com.sellora.partner.repositories.ServicesRepository
import kotlinx.coroutines.launch

class AddServiceViewModel : ViewModel() {
    private val servicesRepository = ServicesRepository()

    private val _service = MutableLiveData<FreelancerService?>()
    val service: LiveData<FreelancerService?> = _service

    private val _saveStatus = MutableLiveData<SaveStatus>()
    val saveStatus: LiveData<SaveStatus> = _saveStatus

    sealed class SaveStatus {
        object Idle : SaveStatus()
        object Loading : SaveStatus()
        object Uploading : SaveStatus()
        object Saving : SaveStatus()
        object Success : SaveStatus()
        data class Error(val message: String) : SaveStatus()
    }

    fun fetchService(serviceId: String) {
        _saveStatus.value = SaveStatus.Loading
        servicesRepository.getServiceById(serviceId)
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    _service.value = servicesRepository.mapDocument(doc.id, doc.data ?: emptyMap())
                }
                _saveStatus.value = SaveStatus.Idle
            }
            .addOnFailureListener { e ->
                _saveStatus.value = SaveStatus.Error(e.message ?: "Failed to fetch service")
            }
    }

    fun saveService(
        context: Context,
        serviceId: String?,
        partnerId: String,
        partnerName: String,
        partnerPhotoUrl: String,
        name: String,
        description: String,
        category: String,
        minPrice: String,
        maxPrice: String,
        basicPrice: String,
        advPrice: String,
        proPrice: String,
        deliveryTime: String,
        localUri: Uri?,
        uploadedMediaUrl: String?,
        deliverables: List<String>,
        deliverableTiers: List<String>
    ) {
        _saveStatus.value = SaveStatus.Idle
        
        viewModelScope.launch {
            val (mediaUrl, error) = if (localUri != null) {
                _saveStatus.value = SaveStatus.Uploading
                MediaRepository.uploadWithError(context, localUri)
            } else {
                Pair(uploadedMediaUrl ?: "", null)
            }

            if (localUri != null && mediaUrl == null) {
                _saveStatus.value = SaveStatus.Error(error ?: "Upload failed")
                return@launch
            }

            _saveStatus.value = SaveStatus.Saving
            servicesRepository.saveService(
                serviceId = serviceId,
                partnerId = partnerId,
                partnerName = partnerName,
                partnerPhotoUrl = partnerPhotoUrl,
                name = name,
                description = description,
                category = category,
                minPrice = minPrice,
                maxPrice = maxPrice,
                basicPrice = basicPrice,
                advPrice = advPrice,
                proPrice = proPrice,
                deliveryTime = deliveryTime,
                imageUri = mediaUrl ?: "",
                deliverables = deliverables,
                deliverableTiers = deliverableTiers
            ).addOnSuccessListener {
                _saveStatus.value = SaveStatus.Success
            }.addOnFailureListener { e ->
                _saveStatus.value = SaveStatus.Error(e.message ?: "Saving failed")
            }
        }
    }
}
