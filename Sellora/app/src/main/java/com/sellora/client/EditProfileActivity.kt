package com.sellora.client

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sellora.client.databinding.ActivityEditProfileBinding
import com.sellora.client.repositories.MediaRepository
import com.sellora.client.repositories.UserRepository
import kotlinx.coroutines.launch
import kotlin.coroutines.resume

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private var selectedImageUri: Uri? = null
    private val auth           = FirebaseAuth.getInstance()
    private val db             = FirebaseFirestore.getInstance()
    private val userRepository = UserRepository()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Show preview immediately
            binding.imgAvatar.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uid = auth.currentUser?.uid
        if (uid != null) {
            userRepository.getCurrentUserProfile(uid) { data, _ ->
                if (data != null) {
                    binding.etFullName.setText(data["name"]    as? String ?: "")
                    binding.etEmail.setText(data["email"]      as? String ?: "")
                    binding.etPhone.setText(data["phone"]      as? String ?: "")
                    binding.imgAvatar.loadImage(data["photoUrl"] as? String, R.drawable.ic_profile_placeholder)
                }
            }
        }

        binding.cameraBadge.setOnClickListener { imagePickerLauncher.launch("image/*") }
        binding.imgAvatar.setOnClickListener   { imagePickerLauncher.launch("image/*") }
        binding.btnBack.setOnClickListener     { finish() }
        binding.btnCancel.setOnClickListener   { finish() }

        binding.btnUpdate.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email    = binding.etEmail.text.toString().trim()
            val phone    = binding.etPhone.text.toString().trim()

            if (fullName.isEmpty() || email.isEmpty()) {
                Snackbar.make(binding.root, "Name and Email are required", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val currentUid = auth.currentUser?.uid ?: run {
                Snackbar.make(binding.root, "Not logged in", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            binding.btnUpdate.isEnabled = false
            binding.btnUpdate.text      = "Updating..."

            lifecycleScope.launch {
                // ✅ Bug 7 fix — upload to Cloudinary instead of encoding as Base64.
                // Base64 in Firestore balloons document size by ~35%, blows the 1MB
                // doc limit for large images, and makes every user read painfully slow.
                val photoUrl: String? = if (selectedImageUri != null) {
                    binding.btnUpdate.text = "Uploading photo…"
                    val url = MediaRepository.upload(this@EditProfileActivity, selectedImageUri!!)
                    if (url == null) {
                        binding.btnUpdate.isEnabled = true
                        binding.btnUpdate.text = "Update"
                        Snackbar.make(binding.root, "Photo upload failed. Try again.", Snackbar.LENGTH_LONG).show()
                        return@launch
                    }
                    url
                } else {
                    // Fetch the existing photoUrl so we don't overwrite it with null
                    userRepository.getCurrentUserProfile(currentUid) { _, _ -> }
                    val snap = db.collection("users").document(currentUid).get().await()
                    snap?.getString("photoUrl")
                }

                saveProfile(currentUid, fullName, email, phone, photoUrl)
            }
        }
    }

    private fun saveProfile(
        uid: String, fullName: String, email: String, phone: String, photoUrl: String?
    ) {
        val updates = mutableMapOf<String, Any>(
            "name"    to fullName, "email"   to email,   "phone" to phone
        )
        if (photoUrl != null) updates["photoUrl"] = photoUrl

        db.collection("users").document(uid).update(updates)
            .addOnSuccessListener {
                getSharedPreferences("sellora_client_auth", MODE_PRIVATE).edit().apply {
                    putString("user_name",  fullName)
                    putString("user_email", email)
                    putString("user_phone", phone)
                    if (photoUrl != null) putString("profile_pic_url", photoUrl)
                    apply()
                }
                Snackbar.make(binding.root, "Profile updated successfully!", Snackbar.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                binding.btnUpdate.isEnabled = true
                binding.btnUpdate.text = "Update"
                Snackbar.make(binding.root, "Update failed: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
    }

    // Tiny suspend helper so we can await() a Firestore Task inline
    private suspend fun com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot>.await()
        : com.google.firebase.firestore.DocumentSnapshot? =
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            addOnSuccessListener { cont.resume(it) }
            addOnFailureListener { cont.resume(null) }
        }
}
