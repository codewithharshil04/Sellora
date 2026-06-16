package com.sellora.partner

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.sellora.partner.databinding.ActivityEditProfileBinding
import com.sellora.partner.viewmodels.EditProfileViewModel
import com.sellora.partner.repositories.AuthRepository

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private var selectedImageUri: Uri? = null
    private val viewModel: EditProfileViewModel by viewModels()

    private val imagePickerLauncher = registerForActivityResult(
        PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.imgAvatar.loadProfileImage(it, R.drawable.ic_profile_placeholder)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val authPrefs = getSharedPreferences("sellora_partner_auth", MODE_PRIVATE)

        // Load cached values instantly
        binding.etFullName.setText(authPrefs.getString("user_name", ""))
        binding.etEmail.setText(authPrefs.getString("user_email", ""))
        binding.etPhone.setText(authPrefs.getString("user_phone", ""))
        binding.imgAvatar.loadProfileImage(
            authPrefs.getString("user_profile_pic", null),
            R.drawable.ic_profile_placeholder
        )

        observeViewModel()

        val uid = AuthRepository().getCurrentUserUid()
        if (uid != null) {
            viewModel.fetchBio(uid)
        }

        binding.cameraBadge.setOnClickListener { 
            imagePickerLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) 
        }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }

        binding.btnResetPassword.setOnClickListener {
            val email = AuthRepository().getCurrentUserEmail() ?: return@setOnClickListener
            com.google.firebase.auth.FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.reset_email_sent), Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, getString(R.string.reset_email_failed), Toast.LENGTH_SHORT).show()
                }
        }

        binding.btnUpdate.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val bio = binding.etBio.text.toString().trim()

            if (fullName.isEmpty() || email.isEmpty()) {
                Snackbar.make(binding.root, getString(R.string.error_name_email_required), Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val currentUid = AuthRepository().getCurrentUserUid() ?: return@setOnClickListener
            val currentPhotoUrl = authPrefs.getString("user_profile_pic", null)

            viewModel.updateProfile(
                this, currentUid, fullName, email, phone, bio, selectedImageUri, currentPhotoUrl
            )
        }
    }

    private fun observeViewModel() {
        viewModel.bio.observe(this) { bio ->
            binding.etBio.setText(bio)
        }

        viewModel.updateStatus.observe(this) { status ->
            when (status) {
                is EditProfileViewModel.UpdateStatus.Loading -> {
                    binding.btnUpdate.isEnabled = false
                    binding.btnUpdate.text = status.message
                }
                is EditProfileViewModel.UpdateStatus.Success -> {
                    updateSharedPrefs(status.photoUrl)
                    Toast.makeText(this, getString(R.string.msg_profile_updated), Toast.LENGTH_SHORT).show()
                    finish()
                }
                is EditProfileViewModel.UpdateStatus.Error -> {
                    binding.btnUpdate.isEnabled = true
                    binding.btnUpdate.text = getString(R.string.btn_update)
                    Snackbar.make(binding.root, status.message, Snackbar.LENGTH_LONG).show()
                }
                else -> {
                    binding.btnUpdate.isEnabled = true
                    binding.btnUpdate.text = getString(R.string.btn_update)
                }
            }
        }
    }

    private fun updateSharedPrefs(newPhotoUrl: String?) {
        val authPrefs = getSharedPreferences("sellora_partner_auth", MODE_PRIVATE)
        authPrefs.edit {
            putString("user_name", binding.etFullName.text.toString().trim())
            putString("user_email", binding.etEmail.text.toString().trim())
            putString("user_phone", binding.etPhone.text.toString().trim())
            if (newPhotoUrl != null) {
                putString("user_profile_pic", newPhotoUrl)
            }
        }
    }
}
