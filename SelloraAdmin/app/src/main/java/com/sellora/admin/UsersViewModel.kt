package com.sellora.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class UsersViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _users = MutableLiveData<List<AdminUser>>()
    val users: LiveData<List<AdminUser>> = _users

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchUsers() {
        _isLoading.value = true
        db.collection("users").whereEqualTo("role", "client").get()
            .addOnSuccessListener { snap ->
                _isLoading.value = false
                val fetchedUsers = snap.documents.map { doc ->
                    AdminUser(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        phone = doc.getString("phone") ?: "",
                        role = doc.getString("role") ?: "client"
                    )
                }
                _users.value = fetchedUsers
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _error.value = e.message ?: "Unknown error"
            }
    }

    fun clearError() { _error.value = null }
}
