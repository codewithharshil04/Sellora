package com.sellora.partner.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.DocumentSnapshot
import com.sellora.partner.repositories.AuthRepository
import com.sellora.partner.repositories.PartnerRepository

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository()
    private val partnerRepository = PartnerRepository()

    private val _loginStatus = MutableLiveData<LoginStatus>()
    val loginStatus: LiveData<LoginStatus> = _loginStatus

    sealed class LoginStatus {
        object Idle : LoginStatus()
        object Loading : LoginStatus()
        object Success : LoginStatus()
        data class Error(val message: String) : LoginStatus()
    }

    fun login(email: String, password: String) {
        _loginStatus.value = LoginStatus.Loading
        authRepository.login(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    fetchProfile(uid)
                } else {
                    _loginStatus.value = LoginStatus.Error("User ID not found")
                }
            }
            .addOnFailureListener { e ->
                _loginStatus.value = LoginStatus.Error(e.message ?: "Login failed")
            }
    }

    private fun fetchProfile(uid: String) {
        partnerRepository.getPartnerProfile(uid)
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    saveToPrefs(document)
                    _loginStatus.value = LoginStatus.Success
                } else {
                    authRepository.logout()
                    _loginStatus.value = LoginStatus.Error("Profile not found")
                }
            }
            .addOnFailureListener { e ->
                authRepository.logout()
                _loginStatus.value = LoginStatus.Error(e.message ?: "Failed to fetch profile")
            }
    }

    private fun saveToPrefs(document: DocumentSnapshot) {
        val context = getApplication<Application>().applicationContext
        val prefs = context.getSharedPreferences("sellora_partner_auth", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_name", document.getString("name"))
            putString("user_email", document.getString("email"))
            putString("user_phone", document.getString("phone"))
            putString("user_username", document.getString("username"))
            putString("user_pan", document.getString("pan"))
            putString("user_profile_pic", document.getString("photoUrl"))
            apply()
        }
    }
}
