package com.sellora.partner.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sellora.partner.repositories.AuthRepository
import com.sellora.partner.repositories.PartnerRepository

class SignupViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository()
    private val partnerRepository = PartnerRepository()

    private val _signupStatus = MutableLiveData<SignupStatus>()
    val signupStatus: LiveData<SignupStatus> = _signupStatus

    sealed class SignupStatus {
        object Idle : SignupStatus()
        object Loading : SignupStatus()
        object Success : SignupStatus()
        data class Error(val message: String) : SignupStatus()
    }

    fun signup(
        username: String,
        fullName: String,
        phone: String,
        email: String,
        pan: String,
        password: String
    ) {
        _signupStatus.value = SignupStatus.Loading
        authRepository.signup(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    saveProfile(uid, username, fullName, phone, email, pan)
                } else {
                    _signupStatus.value = SignupStatus.Error("User ID not found")
                }
            }
            .addOnFailureListener { e ->
                _signupStatus.value = SignupStatus.Error(e.message ?: "Auth failed")
            }
    }

    private fun saveProfile(
        uid: String,
        username: String,
        fullName: String,
        phone: String,
        email: String,
        pan: String
    ) {
        partnerRepository.savePartnerProfile(uid, username, fullName, phone, email, pan)
            .addOnSuccessListener {
                saveToPrefs(username, fullName, phone, email, pan)
                _signupStatus.value = SignupStatus.Success
            }
            .addOnFailureListener { e ->
                _signupStatus.value = SignupStatus.Error(e.message ?: "Save profile failed")
            }
    }

    private fun saveToPrefs(
        username: String,
        fullName: String,
        phone: String,
        email: String,
        pan: String
    ) {
        val context = getApplication<Application>().applicationContext
        val prefs = context.getSharedPreferences("sellora_partner_auth", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_name", fullName)
            putString("user_email", email)
            putString("user_phone", phone)
            putString("user_username", username)
            putString("user_pan", pan)
            apply()
        }
    }
}
