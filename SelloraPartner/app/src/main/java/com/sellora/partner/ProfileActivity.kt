package com.sellora.partner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.sellora.partner.databinding.ActivityProfileBinding
import com.sellora.partner.repositories.OrdersRepository
import com.sellora.partner.viewmodels.ProfileViewModel

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        observeViewModel()
        setupButtons()
    }

    private fun setupButtons() {
        binding.avatarContainer.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binding.btnUpdateProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binding.btnWithdraw.setOnClickListener {
            Toast.makeText(this, getString(R.string.msg_withdraw_coming_soon), Toast.LENGTH_SHORT).show()
        }

        binding.rowContactAdmin.setOnClickListener {
            contactAdmin()
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            getSharedPreferences("sellora_partner_auth", MODE_PRIVATE).edit().clear().apply()
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.rowMyServices.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        binding.rowMyProjects.setOnClickListener {
            startActivity(Intent(this, ProjectsActivity::class.java))
        }

        binding.rowPaymentMethods.setOnClickListener {
            Toast.makeText(this, getString(R.string.msg_payments_coming_soon), Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.profileData.observe(this) { data ->
            val prefs = getSharedPreferences("sellora_partner_auth", MODE_PRIVATE)
            binding.txtName.text = data.name
            binding.txtUsername.text = if (data.username.isNotEmpty()) "@${data.username}" else ""
            binding.txtPan.text = data.pan
            binding.imgAvatar.loadProfileImage(data.photoUrl, R.drawable.ic_profile_placeholder)

            prefs.edit().apply {
                putString("user_name", data.name)
                putString("user_username", data.username)
                putString("user_pan", data.pan)
                putString("user_profile_pic", data.photoUrl)
                apply()
            }
        }

        viewModel.totalIncome.observe(this) { total ->
            binding.txtTotalIncome.text = getString(
                R.string.total_income_format,
                total * Constants.PLATFORM_FEE_RATE
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {
        val prefs    = getSharedPreferences("sellora_partner_auth", MODE_PRIVATE)
        val name     = prefs.getString("user_name",     "") ?: ""
        val username = prefs.getString("user_username", "") ?: ""
        val pan      = prefs.getString("user_pan",      "") ?: ""

        binding.txtName.text     = name
        binding.txtUsername.text = if (username.isNotEmpty()) "@$username" else ""
        binding.txtPan.text      = pan

        val savedPic = prefs.getString("user_profile_pic", null)
        binding.imgAvatar.loadProfileImage(savedPic, R.drawable.ic_profile_placeholder)

        val userId = auth.currentUser?.uid ?: return
        viewModel.fetchProfile(userId)
        viewModel.fetchIncome(userId)
    }

    private fun contactAdmin() {
        val partnerName = binding.txtName.text.toString()
        val partnerEmail = auth.currentUser?.email ?: ""
        
        val uriText = "mailto:admin@sellora.com" +
                "?subject=" + Uri.encode("Partner Support Request") +
                "&body=" + Uri.encode("Hello Sellora Admin,\n\n" +
                "I am reaching out regarding...\n\n" +
                "Partner Details:\n" +
                "Name: $partnerName\n" +
                "Email: $partnerEmail")

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse(uriText)
        }

        try {
            startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }
}
