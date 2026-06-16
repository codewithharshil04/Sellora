package com.sellora.client

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sellora.client.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Real-time validation
        ValidationUtils.setupRealTimeValidation(binding.tilEmail, ValidationType.EMAIL)
        ValidationUtils.setupRealTimeValidation(binding.tilPassword, ValidationType.PASSWORD)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString().orEmpty()

            val isEmailValid = ValidationUtils.validateField(binding.tilEmail, ValidationType.EMAIL)
            val isPasswordValid = ValidationUtils.validateField(binding.tilPassword, ValidationType.PASSWORD)

            if (!isEmailValid || !isPasswordValid) return@setOnClickListener

            binding.btnLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: ""
                        
                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { document ->
                                if (document != null && document.exists()) {
                                    val name = document.getString("name") ?: ""
                                    val phone = document.getString("phone") ?: ""
                                    val userEmail = document.getString("email") ?: email

                                    getSharedPreferences("sellora_client_auth", MODE_PRIVATE)
                                        .edit()
                                        .putBoolean("is_logged_in", true)
                                        .putString("user_name", name)
                                        .putString("user_email", userEmail)
                                        .putString("user_phone", phone)
                                        .apply()

                                    Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, HomeActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                } else {
                                    binding.btnLogin.isEnabled = true
                                    auth.signOut()
                                    Snackbar.make(binding.root, getString(R.string.profile_not_found), Snackbar.LENGTH_LONG).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                binding.btnLogin.isEnabled = true
                                auth.signOut()
                                Snackbar.make(binding.root, getString(R.string.error_fetching_profile, e.message ?: ""), Snackbar.LENGTH_LONG).show()
                            }
                    } else {
                        binding.btnLogin.isEnabled = true
                        val errorMsg = task.exception?.message ?: getString(R.string.auth_failed)
                        Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_LONG).show()
                    }
                }
        }

        binding.txtSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}