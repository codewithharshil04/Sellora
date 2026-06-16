package com.sellora.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore

class DashboardViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _userCount = MutableLiveData<Long>()
    val userCount: LiveData<Long> = _userCount

    private val _partnerCount = MutableLiveData<Long>()
    val partnerCount: LiveData<Long> = _partnerCount

    private val _serviceCount = MutableLiveData<Long>()
    val serviceCount: LiveData<Long> = _serviceCount

    private val _orderCount = MutableLiveData<Long>()
    val orderCount: LiveData<Long> = _orderCount

    private val _pendingCount = MutableLiveData<Long>()
    val pendingCount: LiveData<Long> = _pendingCount

    private val _deliveredCount = MutableLiveData<Long>()
    val deliveredCount: LiveData<Long> = _deliveredCount

    private val _cancelledCount = MutableLiveData<Long>()
    val cancelledCount: LiveData<Long> = _cancelledCount

    private val _revenue = MutableLiveData<String>()
    val revenue: LiveData<String> = _revenue

    fun loadStats() {
        countCollection("users", "role", "client") { _userCount.value = it }
        countCollection("partners", null, null) { _partnerCount.value = it }
        countCollection("services", "isActive", true) { _serviceCount.value = it }

        db.collection("orders").count().get(AggregateSource.SERVER).addOnSuccessListener {
            _orderCount.value = it.count
        }

        db.collection("orders").whereIn("status", listOf("New", "Pending")).count().get(AggregateSource.SERVER).addOnSuccessListener {
            _pendingCount.value = it.count
        }

        db.collection("orders").whereEqualTo("status", "Delivered").count().get(AggregateSource.SERVER).addOnSuccessListener {
            _deliveredCount.value = it.count
        }

        db.collection("orders").whereEqualTo("status", "Cancelled").count().get(AggregateSource.SERVER).addOnSuccessListener {
            _cancelledCount.value = it.count
        }

        calculateRevenue()
    }

    private fun calculateRevenue() {
        db.collection("orders").whereEqualTo("status", "Delivered").get()
            .addOnSuccessListener { snap ->
                var revenue = 0.0
                for (doc in snap) {
                    val priceStr = doc.getString("price") ?: "0"
                    val price = priceStr.replace(Regex("[^\\d.]"), "").toDoubleOrNull() ?: 0.0
                    revenue += price
                }
                _revenue.value = "₹%.0f".format(revenue * 0.9)
            }
    }

    private fun countCollection(collection: String, field: String?, value: Any?, onResult: (Long) -> Unit) {
        val query = if (field != null && value != null)
            db.collection(collection).whereEqualTo(field, value)
        else
            db.collection(collection)

        query.count().get(AggregateSource.SERVER)
            .addOnSuccessListener { onResult(it.count) }
            .addOnFailureListener { onResult(0L) }
    }
}
