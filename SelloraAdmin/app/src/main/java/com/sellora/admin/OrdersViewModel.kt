package com.sellora.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class OrdersViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    private val _orders = MutableLiveData<List<AdminOrder>>()
    val orders: LiveData<List<AdminOrder>> = _orders

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchOrders(currentStatus: String) {
        _isLoading.value = true
        
        val query = if (currentStatus == "New") {
            db.collection("orders").whereIn("status", listOf("New", "Pending"))
        } else {
            db.collection("orders").whereEqualTo("status", currentStatus)
        }

        query.get()
            .addOnSuccessListener { snap ->
                _isLoading.value = false
                val fetchedOrders = snap.documents.map { doc ->
                    val ts = doc.getTimestamp("createdAt")
                    AdminOrder(
                        id = doc.id,
                        serviceName = doc.getString("serviceName") ?: "",
                        clientName = doc.getString("clientName") ?: "",
                        partnerName = doc.getString("partnerName") ?: doc.getString("freelancerName") ?: "",
                        price = doc.getString("price") ?: "₹0",
                        status = doc.getString("status") ?: "New",
                        date = if (ts != null) sdf.format(ts.toDate()) else "—",
                        packageType = doc.getString("packageType") ?: ""
                    )
                }
                _orders.value = fetchedOrders
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _error.value = e.message ?: "Unknown error"
            }
    }

    fun updateOrderStatus(orderId: String, newStatus: String, onComplete: (Boolean, String?) -> Unit) {
        db.collection("orders").document(orderId).update("status", newStatus)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, e.message)
            }
    }

    fun clearError() { _error.value = null }
}
