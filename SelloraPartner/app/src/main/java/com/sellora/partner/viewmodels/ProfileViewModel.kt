package com.sellora.partner.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.sellora.partner.repositories.OrdersRepository

class ProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val ordersRepository = OrdersRepository()

    private val _profileData = MutableLiveData<ProfileData>()
    val profileData: LiveData<ProfileData> = _profileData

    private val _totalIncome = MutableLiveData<Double>()
    val totalIncome: LiveData<Double> = _totalIncome

    data class ProfileData(
        val name: String,
        val username: String,
        val pan: String,
        val photoUrl: String?
    )

    fun fetchProfile(userId: String) {
        db.collection("partners").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    _profileData.value = ProfileData(
                        name = doc.getString("name") ?: "",
                        username = doc.getString("username") ?: "",
                        pan = doc.getString("pan") ?: "",
                        photoUrl = doc.getString("photoUrl")
                    )
                }
            }
    }

    fun fetchIncome(userId: String) {
        ordersRepository.fetchOrdersByPartner(userId)
            .addOnSuccessListener { documents ->
                var total = 0.0
                for (doc in documents) {
                    val status = doc.getString("status") ?: ""
                    if (status == "Delivered") {
                        val price = doc.get("price")?.toString()
                            ?.replace("₹", "")?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                        total += price
                    }
                }
                _totalIncome.value = total
            }
    }
}
