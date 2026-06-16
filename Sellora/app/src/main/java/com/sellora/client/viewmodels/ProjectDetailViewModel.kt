package com.sellora.client.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sellora.client.repositories.OrdersRepository

class ProjectDetailViewModel : ViewModel() {

    private val ordersRepository = OrdersRepository()
    private val db = FirebaseFirestore.getInstance()
    private var orderListener: ListenerRegistration? = null

    private val _orderDetails = MutableLiveData<Map<String, Any>?>()
    val orderDetails: LiveData<Map<String, Any>?> = _orderDetails

    private val _cancelResult = MutableLiveData<Boolean>()
    val cancelResult: LiveData<Boolean> = _cancelResult

    fun startOrderListener(orderId: String) {
        orderListener?.remove()
        orderListener = db.collection("orders").document(orderId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) {
                    _orderDetails.value = null
                    return@addSnapshotListener
                }
                _orderDetails.value = snapshot.data
            }
    }

    fun fetchOrderDetails(orderId: String) {
        db.collection("orders").document(orderId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    _orderDetails.value = doc.data
                }
            }
    }

    fun cancelOrder(orderId: String) {
        ordersRepository.updateOrderStatus(orderId, "Cancelled") { success, _ ->
            _cancelResult.value = success
        }
    }

    override fun onCleared() {
        super.onCleared()
        orderListener?.remove()
    }
}
