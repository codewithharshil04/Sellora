package com.sellora.partner

import android.util.Patterns
import android.widget.EditText
import android.text.TextUtils
import java.util.regex.Pattern

/**
 * Utility class for form validation across the Sellora Partner app
 * Provides consistent validation patterns and error messages
 */
object ValidationUtils {
    
    // Email validation
    fun isValidEmail(email: String): Boolean {
        return !TextUtils.isEmpty(email) && 
                Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    fun getEmailError(email: String): String {
        return when {
            email.isEmpty() -> "Email is required"
            !isValidEmail(email) -> "Please enter a valid email address"
            else -> ""
        }
    }
    
    // Phone validation (India: 10-digit mobile numbers)
    fun isValidPhone(phone: String): Boolean {
        return !TextUtils.isEmpty(phone) && 
                phone.matches(Regex("^[6-9]\\d{9}$"))
    }
    
    fun getPhoneError(phone: String): String {
        return when {
            phone.isEmpty() -> "Phone number is required"
            !isValidPhone(phone) -> "Please enter a valid 10-digit phone number"
            else -> ""
        }
    }
    
    // Password validation
    fun isValidPassword(password: String): Boolean {
        return !TextUtils.isEmpty(password) && 
                password.length >= 6 && 
                password.matches(Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{6,}$"))
    }
    
    fun getPasswordError(password: String): String {
        return when {
            password.isEmpty() -> "Password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            !isValidPassword(password) -> "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
            else -> ""
        }
    }
    
    // Name validation
    fun isValidName(name: String): Boolean {
        return !TextUtils.isEmpty(name) && 
                name.length >= 2 && 
                name.length <= 50 && 
                name.matches(Regex("^[a-zA-Z\\s]*$"))
    }
    
    fun getNameError(name: String): String {
        return when {
            name.isEmpty() -> "Name is required"
            name.length < 2 -> "Name must be at least 2 characters"
            name.length > 50 -> "Name must be 50 characters or less"
            !isValidName(name) -> "Name can only contain letters"
            else -> ""
        }
    }
    
    // Service name validation
    fun isValidServiceName(name: String): Boolean {
        return !TextUtils.isEmpty(name) && 
                name.length >= 3 && 
                name.length <= 100 && 
                name.matches(Regex("^[a-zA-Z0-9\\s]*$"))
    }
    
    fun getServiceNameError(name: String): String {
        return when {
            name.isEmpty() -> "Service name is required"
            name.length < 3 -> "Service name must be at least 3 characters"
            name.length > 100 -> "Service name must be 100 characters or less"
            else -> ""
        }
    }
    
    // Price validation
    fun isValidPrice(price: String): Boolean {
        return try {
            val amount = price.replace("[^0-9.]".toRegex(), "").toDoubleOrNull()
            amount != null && amount > 0
        } catch (e: Exception) {
            false
        }
    }
    
    fun getPriceError(price: String): String {
        return when {
            price.isEmpty() -> "Price is required"
            !isValidPrice(price) -> "Please enter a valid price"
            else -> ""
        }
    }
    
    // PAN Card validation (India)
    fun isValidPAN(pan: String): Boolean {
        return !TextUtils.isEmpty(pan) && 
                pan.matches(Regex("^[A-Z]{5}[0-9]{4}[A-Z]$"))
    }
    
    fun getPANError(pan: String): String {
        return when {
            pan.isEmpty() -> "PAN number is required"
            !isValidPAN(pan) -> "Please enter a valid PAN number (e.g. ABCDE1234F)"
            else -> ""
        }
    }
    
    // Generic field validation
    fun isFieldValid(value: String): Boolean {
        return !TextUtils.isEmpty(value.trim())
    }
    
    fun getRequiredFieldError(fieldName: String): String {
        return "$fieldName is required"
    }
    
    // Real-time validation for EditText
    fun setupRealTimeValidation(editText: EditText, validationType: ValidationType) {
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateField(editText, validationType)
            }
        }
    }
    
    private fun validateField(editText: EditText, validationType: ValidationType) {
        val value = editText.text.toString()
        val error = when (validationType) {
            ValidationType.EMAIL -> getEmailError(value)
            ValidationType.PHONE -> getPhoneError(value)
            ValidationType.PASSWORD -> getPasswordError(value)
            ValidationType.NAME -> getNameError(value)
            ValidationType.SERVICE_NAME -> getServiceNameError(value)
            ValidationType.PRICE -> getPriceError(value)
            ValidationType.PAN -> getPANError(value)
            ValidationType.REQUIRED -> getRequiredFieldError(editText.hint.toString())
        }
        
        editText.error = if (error.isNotEmpty()) error else null
    }
}
