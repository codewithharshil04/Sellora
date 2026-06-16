package com.sellora.client.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.sellora.client.repositories.UserRepository

class ProfileViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _profileData = MutableLiveData<Map<String, Any>?>()
    val profileData: LiveData<Map<String, Any>?> = _profileData

    private var profileListener: ListenerRegistration? = null

    fun startProfileObserver() {
        val uid = auth.currentUser?.uid ?: return
        profileListener = userRepository.observeUserProfile(uid) { data ->
            _profileData.value = data
        }
    }

    fun signOut() {
        auth.signOut()
    }

    override fun onCleared() {
        super.onCleared()
        profileListener?.remove()
    }
}
