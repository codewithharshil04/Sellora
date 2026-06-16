package com.sellora.client

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sellora.client.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.btnSignup.setOnClickListener {
            performSignup()
        }

        // Setup real-time validation
        ValidationUtils.setupRealTimeValidation(binding.tilFullName, ValidationType.NAME)
        ValidationUtils.setupRealTimeValidation(binding.tilPhone, ValidationType.PHONE)
        ValidationUtils.setupRealTimeValidation(binding.tilEmail, ValidationType.EMAIL)
        ValidationUtils.setupRealTimeValidation(binding.tilPassword, ValidationType.PASSWORD)
        
        setupLoginButton()
    }

    private fun setupLoginButton() {
        binding.txtLogin.setOnClickListener {
            LoadingUtils.scalePress(binding.txtLogin)
            startActivity(Intent(this, LoginActivity::class.java))
            LoadingUtils.scaleRelease(binding.txtLogin)
        }
    }

    private fun performSignup() {
        val fullName = binding.etFullName.text.toString().trim()
        val phone    = binding.etPhone.text.toString().trim()
        val actualEmail = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        // Validate using ValidationUtils
        val isNameValid = ValidationUtils.validateField(binding.tilFullName, ValidationType.NAME)
        val isPhoneValid = ValidationUtils.validateField(binding.tilPhone, ValidationType.PHONE)
        val isEmailValid = ValidationUtils.validateField(binding.tilEmail, ValidationType.EMAIL)
        val isPasswordValid = ValidationUtils.validateField(binding.tilPassword, ValidationType.PASSWORD)
        
        if (isNameValid && isPhoneValid && isEmailValid && isPasswordValid) {
            LoadingUtils.showLoading(binding.btnSignup, getString(R.string.creating_account))
            
            auth.createUserWithEmailAndPassword(actualEmail, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: ""

                        val userProfile = hashMapOf(
                            "uid" to uid,
                            "name" to fullName,
                            "email" to actualEmail,
                            "phone" to phone,
                            "role" to "client",
                            "favoriteServiceIds" to arrayListOf<String>()
                        )

                        db.collection("users").document(uid)
                            .set(userProfile)
                            .addOnSuccessListener {
                                getSharedPreferences("sellora_client_auth", Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("user_name",  fullName)
                                    .putString("user_email", actualEmail)
                                    .putString("user_phone", phone)
                                    .putBoolean("is_logged_in", true)
                                    .apply()

                                Toast.makeText(this, getString(R.string.account_created), Toast.LENGTH_SHORT).show()

                                val intent = Intent(this, HomeActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                LoadingUtils.hideLoading(binding.btnSignup)
                                LoadingUtils.shakeError(binding.tilFullName)
                                Toast.makeText(this, getString(R.string.generic_error, e.message ?: ""), Toast.LENGTH_LONG).show()
                            }
                    } else {
                        LoadingUtils.hideLoading(binding.btnSignup)
                        LoadingUtils.shakeError(binding.tilEmail)
                        Toast.makeText(baseContext, task.exception?.message ?: getString(R.string.signup_failed), Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}