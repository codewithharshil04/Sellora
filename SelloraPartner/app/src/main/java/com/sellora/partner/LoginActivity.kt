package com.sellora.partner

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.sellora.partner.databinding.ActivityLoginBinding
import com.sellora.partner.viewmodels.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPasswordToggle()
        setupLoginButton()
        setupSignupLink()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.loginStatus.observe(this) { status ->
            when (status) {
                is LoginViewModel.LoginStatus.Loading -> {
                    binding.btnLogin.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is LoginViewModel.LoginStatus.Success -> {
                    binding.progressBar.visibility = View.GONE
                    navigateToDashboard()
                }
                is LoginViewModel.LoginStatus.Error -> {
                    binding.btnLogin.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, status.message, Toast.LENGTH_LONG).show()
                }
                is LoginViewModel.LoginStatus.Idle -> {
                    binding.btnLogin.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun setupPasswordToggle() {
        binding.tilPassword.setEndIconOnClickListener {
            isPasswordVisible = !isPasswordVisible

            binding.etPassword.inputType = if (isPasswordVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            binding.tilPassword.setEndIconDrawable(
                if (isPasswordVisible) R.drawable.ic_eye_slash else R.drawable.ic_eye
            )

            binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
        }
    }

    private fun setupLoginButton() {
        binding.btnLogin.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (!validateLogin(email, password)) return@setOnClickListener

            viewModel.login(email, password)
        }
    }

    private fun validateLogin(email: String, password: String): Boolean {
        binding.tilEmail.error    = null
        binding.tilPassword.error = null

        var isValid = true
        if (!ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.error = ValidationUtils.getEmailError(email)
            isValid = false
        }
        
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_password_required)
            isValid = false
        }
        return isValid
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupSignupLink() {
        binding.txtBecomeFreelancer.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
