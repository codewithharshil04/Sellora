package com.sellora.admin

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.sellora.admin.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()

        // If already logged in as admin, go straight to dashboard
        val uid = auth.currentUser?.uid
        if (uid != null) {
            viewModel.verifyAdmin(uid)
            return
        }

        setupPasswordToggle()
        setupLoginButton()
        MotionUtils.applyPressEffect(binding.btnLogin)
    }

    private fun setupObservers() {
        viewModel.loginStatus.observe(this) { status ->
            when (status) {
                is LoginStatus.Idle -> setLoading(false)
                is LoginStatus.Loading -> setLoading(true)
                is LoginStatus.Success -> {
                    startActivity(Intent(this, DashboardActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                    finish()
                }
                is LoginStatus.Error -> {
                    setLoading(false)
                    Snackbar.make(binding.root, status.message, Snackbar.LENGTH_LONG).show()
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
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            binding.tilEmail.error = null
            binding.tilPassword.error = null

            var valid = true
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Enter a valid email"
                MotionUtils.shakeError(binding.tilEmail)
                valid = false
            }
            if (password.isEmpty()) {
                binding.tilPassword.error = "Password is required"
                MotionUtils.shakeError(binding.tilPassword)
                valid = false
            }
            if (!valid) return@setOnClickListener

            setLoading(true)

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val uid = auth.currentUser?.uid ?: run {
                        setLoading(false); return@addOnSuccessListener
                    }
                    viewModel.verifyAdmin(uid)
                }
                .addOnFailureListener { e ->
                    setLoading(false)
                    Snackbar.make(binding.root, e.message ?: "Login failed", Snackbar.LENGTH_LONG).show()
                }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
        binding.btnLogin.text = if (isLoading) "" else "Sign In"
        binding.pbLoading.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
    }
}
