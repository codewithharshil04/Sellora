package com.sellora.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

sealed class LoginStatus {
    object Idle : LoginStatus()
    object Loading : LoginStatus()
    object Success : LoginStatus()
    data class Error(val message: String) : LoginStatus()
}

class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _loginStatus = MutableLiveData<LoginStatus>(LoginStatus.Idle)
    val loginStatus: LiveData<LoginStatus> = _loginStatus

    fun verifyAdmin(uid: String) {
        _loginStatus.value = LoginStatus.Loading
        db.collection("admins").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    _loginStatus.value = LoginStatus.Success
                } else {
                    auth.signOut()
                    _loginStatus.value = LoginStatus.Error("Access denied. Admin only.")
                }
            }
            .addOnFailureListener { e ->
                auth.signOut()
                _loginStatus.value = LoginStatus.Error("Error verifying admin: ${e.message}")
            }
    }

    fun resetStatus() {
        _loginStatus.value = LoginStatus.Idle
    }
}
