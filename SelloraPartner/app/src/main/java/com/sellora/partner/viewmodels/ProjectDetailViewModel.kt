package com.sellora.partner.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.sellora.partner.Project
import com.sellora.partner.repositories.OrdersRepository
import com.sellora.partner.repositories.SupabaseRepository
import kotlinx.coroutines.launch
import java.util.Date

class ProjectDetailViewModel : ViewModel() {
    private val ordersRepository = OrdersRepository()
    private var orderListener: ListenerRegistration? = null

    private val _project = MutableLiveData<Project?>()
    val project: LiveData<Project?> = _project

    private val _uploadStatus = MutableLiveData<UploadStatus>(UploadStatus.Idle)
    val uploadStatus: LiveData<UploadStatus> = _uploadStatus

    sealed class UploadStatus {
        object Idle : UploadStatus()
        object Uploading : UploadStatus()
        object Saving : UploadStatus()
        object Success : UploadStatus()
        data class Error(val message: String) : UploadStatus()
    }

    fun startOrderListener(orderId: String) {
        orderListener?.remove()
        orderListener = ordersRepository.listenToOrder(orderId) { updatedProject ->
            _project.value = updatedProject
        }
    }

    fun performDelivery(context: Context, orderId: String, uri: Uri) {
        _uploadStatus.value = UploadStatus.Uploading
        viewModelScope.launch {
            val (fileUrl, error) = SupabaseRepository.uploadFile(context, uri, orderId)
            if (fileUrl == null) {
                _uploadStatus.value = UploadStatus.Error(error ?: "Upload failed")
                return@launch
            }

            _uploadStatus.value = UploadStatus.Saving
            val updates = mapOf(
                "status"          to "Delivered",
                "deliveryFileUrl" to fileUrl,
                "updatedAt"       to Date()
            )
            
            ordersRepository.updateOrder(orderId, updates)
                .addOnSuccessListener {
                    _uploadStatus.value = UploadStatus.Success
                }
                .addOnFailureListener { e ->
                    _uploadStatus.value = UploadStatus.Error(e.message ?: "Saving failed")
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        orderListener?.remove()
    }
}
