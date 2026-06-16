package com.sellora.client.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sellora.client.SupabaseUploader
import com.sellora.client.repositories.OrdersRepository
import com.sellora.client.repositories.UserRepository
import kotlinx.coroutines.launch
import java.util.UUID

class PlaceOrderViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository()
    private val ordersRepository = OrdersRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _userData = MutableLiveData<Map<String, Any>?>()
    val userData: LiveData<Map<String, Any>?> = _userData

    private val _orderResult = MutableLiveData<Pair<String?, Exception?>>()
    val orderResult: LiveData<Pair<String?, Exception?>> = _orderResult

    private val _isProcessing = MutableLiveData<Boolean>()
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _processingMessage = MutableLiveData<String>()
    val processingMessage: LiveData<String> = _processingMessage

    fun loadUserData() {
        auth.currentUser?.uid?.let { uid ->
            userRepository.getCurrentUserProfile(uid) { data, _ ->
                _userData.value = data
            }
        }
    }

    fun createOrder(orderData: HashMap<String, Any>, selectedFileUri: Uri?) {
        _isProcessing.value = true
        _processingMessage.value = "Processing..."

        viewModelScope.launch {
            if (selectedFileUri != null) {
                _processingMessage.value = "Uploading attachment..."
                val tempOrderId = UUID.randomUUID().toString()
                val fileUrl = SupabaseUploader.uploadFile(
                    getApplication(),
                    selectedFileUri,
                    tempOrderId
                )
                if (fileUrl != null) {
                    orderData["fileUrl"] = fileUrl
                }
            }

            _processingMessage.value = "Finalizing order..."
            ordersRepository.createOrder(orderData) { orderId, exception ->
                _isProcessing.value = false
                _orderResult.value = Pair(orderId, exception)
            }
        }
    }
}
