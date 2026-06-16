package com.sellora.partner.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sellora.partner.repositories.MediaRepository
import kotlinx.coroutines.launch

class EditProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _bio = MutableLiveData<String>()
    val bio: LiveData<String> = _bio

    private val _updateStatus = MutableLiveData<UpdateStatus>()
    val updateStatus: LiveData<UpdateStatus> = _updateStatus

    sealed class UpdateStatus {
        object Idle : UpdateStatus()
        data class Loading(val message: String) : UpdateStatus()
        data class Success(val photoUrl: String?) : UpdateStatus()
        data class Error(val message: String) : UpdateStatus()
    }

    fun fetchBio(uid: String) {
        db.collection("partners").document(uid).get()
            .addOnSuccessListener { doc ->
                _bio.value = doc.getString("bio") ?: ""
            }
    }

    fun updateProfile(
        context: Context,
        uid: String,
        fullName: String,
        email: String,
        phone: String,
        bio: String,
        newImageUri: Uri?,
        currentPhotoUrl: String?
    ) {
        _updateStatus.value = UpdateStatus.Loading("Updating...")

        viewModelScope.launch {
            var photoUrl = currentPhotoUrl
            
            if (newImageUri != null) {
                _updateStatus.value = UpdateStatus.Loading("Uploading image...")
                val (uploadedUrl, error) = MediaRepository.uploadWithError(context, newImageUri)
                if (uploadedUrl == null) {
                    _updateStatus.value = UpdateStatus.Error(error ?: "Photo upload failed")
                    return@launch
                }
                photoUrl = uploadedUrl
            }

            val updates = mutableMapOf<String, Any>(
                "name"  to fullName,
                "email" to email,
                "phone" to phone,
                "bio"   to bio
            )
            if (photoUrl != null) updates["photoUrl"] = photoUrl

            db.collection("partners").document(uid).update(updates)
                .addOnSuccessListener {
                    _updateStatus.value = UpdateStatus.Success(photoUrl)
                }
                .addOnFailureListener { e ->
                    _updateStatus.value = UpdateStatus.Error(e.message ?: "Update failed")
                }
        }
    }
}
