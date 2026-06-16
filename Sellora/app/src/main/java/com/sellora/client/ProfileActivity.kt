package com.sellora.client

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.sellora.client.databinding.ActivityProfileBinding
import com.sellora.client.viewmodels.ProfileViewModel

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load cached info first
        val authPrefs = getSharedPreferences("sellora_client_auth", Context.MODE_PRIVATE)
        binding.txtName.text  = authPrefs.getString("user_name", "")
        binding.txtEmail.text = authPrefs.getString("user_email", "")
        binding.imgAvatar.loadImage(
            authPrefs.getString("profile_pic_url", null),
            R.drawable.ic_profile_placeholder
        )

        setupObservers()
        setupListeners()

        viewModel.startProfileObserver()
    }

    private fun setupObservers() {
        viewModel.profileData.observe(this) { data ->
            if (data != null) {
                val name     = data["name"]     as? String ?: ""
                val email    = data["email"]    as? String ?: ""
                val photoUrl = data["photoUrl"] as? String

                binding.txtName.text  = name
                binding.txtEmail.text = email
                binding.imgAvatar.loadImage(photoUrl, R.drawable.ic_profile_placeholder)

                // Update cache
                getSharedPreferences("sellora_client_auth", Context.MODE_PRIVATE).edit().apply {
                    putString("user_name", name)
                    putString("user_email", email)
                    putString("profile_pic_url", photoUrl)
                    apply()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.avatarContainer.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
        binding.btnUpdateProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
        binding.btnLogout.setOnClickListener {
            viewModel.signOut()
            getSharedPreferences("sellora_client_auth", Context.MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
        binding.rowMyProjects.setOnClickListener {
            startActivity(Intent(this, ProjectsActivity::class.java))
        }
        binding.rowSavedServices.setOnClickListener {
            startActivity(Intent(this, SavedServicesActivity::class.java))
        }

        binding.bottomNav.selectedItemId = R.id.nav_profile
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> true
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    false
                }
                R.id.nav_category -> {
                    val sheet = CategoryBottomSheet()
                    sheet.onCategorySelected = { category ->
                        startActivity(Intent(this, HomeActivity::class.java).apply {
                            putExtra("selected_category", category)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        })
                    }
                    sheet.show(supportFragmentManager, "CategoryBottomSheet")
                    true
                }
                R.id.nav_projects -> {
                    startActivity(Intent(this, ProjectsActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_profile
    }
}
