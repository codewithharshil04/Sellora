package com.sellora.partner

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.sellora.partner.databinding.ActivitySignupBinding
import com.sellora.partner.viewmodels.SignupViewModel
import com.sellora.partner.ValidationUtils
import com.sellora.partner.ValidationType
import com.sellora.partner.LoadingUtils

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val viewModel: SignupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignUp.setOnClickListener {
            performSignup()
        }

        // Setup real-time validation
        ValidationUtils.setupRealTimeValidation(binding.etUsername, ValidationType.NAME)
        ValidationUtils.setupRealTimeValidation(binding.etFullName, ValidationType.NAME)
        ValidationUtils.setupRealTimeValidation(binding.etPhone, ValidationType.PHONE)
        ValidationUtils.setupRealTimeValidation(binding.etEmail, ValidationType.EMAIL)
        ValidationUtils.setupRealTimeValidation(binding.etPanCard, ValidationType.PAN)
        ValidationUtils.setupRealTimeValidation(binding.etPassword, ValidationType.PASSWORD)

        binding.txtLoginHere.setOnClickListener {
            LoadingUtils.scalePress(binding.txtLoginHere)
            startActivity(Intent(this, LoginActivity::class.java))
            LoadingUtils.scaleRelease(binding.txtLoginHere)
            finish()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.signupStatus.observe(this) { status ->
            when (status) {
                is SignupViewModel.SignupStatus.Loading -> {
                    LoadingUtils.showLoading(binding.btnSignUp, "Creating Account...")
                    binding.btnSignUp.isEnabled = false
                }
                is SignupViewModel.SignupStatus.Success -> {
                    LoadingUtils.hideLoading(binding.btnSignUp)
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                is SignupViewModel.SignupStatus.Error -> {
                    LoadingUtils.hideLoading(binding.btnSignUp)
                    binding.btnSignUp.isEnabled = true
                    LoadingUtils.shakeError(binding.tilUsername)
                    Toast.makeText(this, status.message, Toast.LENGTH_LONG).show()
                }
                is SignupViewModel.SignupStatus.Idle -> {
                    LoadingUtils.hideLoading(binding.btnSignUp)
                    binding.btnSignUp.isEnabled = true
                }
            }
        }
    }

    private fun performSignup() {
        val username = binding.etUsername.text.toString().trim()
        val fullName = binding.etFullName.text.toString().trim()
        val phone    = binding.etPhone.text.toString().trim()
        val email    = binding.etEmail.text.toString().trim()
        val panCard  = binding.etPanCard.text.toString().trim().uppercase()
        val password = binding.etPassword.text.toString().trim()

        // Clear previous errors
        binding.tilUsername.error = null
        binding.tilFullName.error = null
        binding.tilPhone.error = null
        binding.tilEmail.error = null
        binding.tilPanCard.error = null
        binding.tilPassword.error = null

        // Validate using ValidationUtils
        var isValid = true

        if (!ValidationUtils.isValidName(username)) {
            binding.tilUsername.error = ValidationUtils.getNameError(username)
            isValid = false
        }

        if (!ValidationUtils.isValidName(fullName)) {
            binding.tilFullName.error = ValidationUtils.getNameError(fullName)
            isValid = false
        }

        if (!ValidationUtils.isValidPhone(phone)) {
            binding.tilPhone.error = ValidationUtils.getPhoneError(phone)
            isValid = false
        }

        if (!ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.error = ValidationUtils.getEmailError(email)
            isValid = false
        }

        if (!ValidationUtils.isValidPAN(panCard)) {
            binding.tilPanCard.error = ValidationUtils.getPANError(panCard)
            isValid = false
        }

        if (!ValidationUtils.isValidPassword(password)) {
            binding.tilPassword.error = ValidationUtils.getPasswordError(password)
            isValid = false
        }

        if (!isValid) return

        viewModel.signup(username, fullName, phone, email, panCard, password)
    }
}
