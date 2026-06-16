package com.sellora.client

import com.google.gson.Gson
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.sellora.client.databinding.ActivityPlaceOrderBinding
import com.sellora.client.viewmodels.PlaceOrderViewModel
import org.json.JSONObject

data class OrderFormData(
    val fullName: String,
    val phone: String,
    val email: String,
    val requirements: String,
    val selectedPlan: String,
    val priceStr: String
)

class PlaceOrderActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var binding: ActivityPlaceOrderBinding
    private var selectedFileUri: Uri? = null
    private val viewModel: PlaceOrderViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private val gson = Gson()

    private var currentOrderData: HashMap<String, Any>? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = copyUriToCache(this, it) ?: it
            binding.txtFileName.text       = getFileName(it)
            binding.txtFileName.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaceOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Checkout.preload(applicationContext)

        setupUI()
        setupObservers()
        loadCachedData()

        binding.btnBack.setOnClickListener           { finish() }
        binding.btnChooseFile.setOnClickListener     { filePickerLauncher.launch("*/*") }
        binding.btnConfirmPayment.setOnClickListener { validateAndInitiatePayment() }

        viewModel.loadUserData()
    }

    private fun setupUI() {
        val preselectedPlan = intent.getStringExtra("selected_plan") ?: "Basic"
        when (preselectedPlan) {
            "Basic" -> binding.togglePlan.check(R.id.btnBasic)
            "Adv"   -> binding.togglePlan.check(R.id.btnAdv)
            "Pro"   -> binding.togglePlan.check(R.id.btnPro)
        }
    }

    private fun setupObservers() {
        viewModel.userData.observe(this) { data ->
            if (data != null) {
                binding.etFullName.setText(data["name"]  as? String ?: "")
                binding.etPhone.setText(data["phone"]    as? String ?: "")
                binding.etEmail.setText(data["email"]    as? String ?: "")
            }
        }

        viewModel.isProcessing.observe(this) { isProcessing ->
            val message = viewModel.processingMessage.value ?: "Processing..."
            setLoading(isProcessing, message)
        }

        viewModel.orderResult.observe(this) { (orderId, exception) ->
            if (orderId != null) {
                val orderData = currentOrderData!!
                startActivity(
                    Intent(this, OrderSuccessActivity::class.java).apply {
                        putExtra("order_id",        orderId)
                        putExtra("service_name",    orderData["serviceName"] as String)
                        putExtra("freelancer_name", orderData["partnerName"] as String)
                        putExtra("selected_plan",   orderData["packageType"] as String)
                        putExtra("price",           orderData["price"] as String)
                    }
                )
                finish()
            } else if (exception != null) {
                Toast.makeText(this, getString(R.string.order_failed, exception.message ?: ""), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadCachedData() {
        val authPrefs = getSharedPreferences("sellora_client_auth", Context.MODE_PRIVATE)
        binding.etFullName.setText(authPrefs.getString("user_name",  ""))
        binding.etPhone.setText(authPrefs.getString("user_phone",    ""))
        binding.etEmail.setText(authPrefs.getString("user_email",    ""))
    }

    private fun validateAndInitiatePayment() {
        val formData = collectFormData()
        if (!validateForm(formData)) return

        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, getString(R.string.login_required), Toast.LENGTH_SHORT).show()
            return
        }

        currentOrderData = buildOrderPayload(formData, currentUser.uid)
        startPayment(extractPrice(formData.priceStr) * 100L, formData.email, formData.phone)
    }

    private fun collectFormData(): OrderFormData {
        val serviceJson = intent.getStringExtra("service_json")
        val service = if (serviceJson != null) gson.fromJson(serviceJson, Service::class.java) else Service()
        val selectedPlan = when (binding.togglePlan.checkedButtonId) {
            R.id.btnBasic -> "Basic"
            R.id.btnAdv   -> "Adv"
            R.id.btnPro   -> "Pro"
            else          -> "Basic"
        }

        val priceStr = when (selectedPlan) {
            "Basic" -> service.basicPrice
            "Adv"   -> service.advPrice
            "Pro"   -> service.proPrice
            else    -> service.basicPrice
        }

        return OrderFormData(
            fullName = binding.etFullName.text.toString().trim(),
            phone = binding.etPhone.text.toString().trim(),
            email = binding.etEmail.text.toString().trim(),
            requirements = binding.etRequirements.text.toString().trim(),
            selectedPlan = selectedPlan,
            priceStr = priceStr
        )
    }

    private fun buildOrderPayload(data: OrderFormData, userId: String): HashMap<String, Any> {
        val serviceJson = intent.getStringExtra("service_json")
        val service = if (serviceJson != null) gson.fromJson(serviceJson, Service::class.java) else Service()
        val imageUrl = service.imageUrl
        val freelancerPhotoUrl = service.freelancerPhotoUrl
        val clientPhotoUrl = getSharedPreferences("sellora_client_auth", MODE_PRIVATE).getString("profile_pic_url", "") ?: ""

        return hashMapOf(
            "serviceId"   to service.id,
            "serviceName" to service.serviceName,
            "clientId"    to userId,
            "clientName"  to data.fullName,
            "partnerId"   to service.partnerId,
            "partnerName" to service.freelancerName,
            "packageType" to data.selectedPlan,
            "price"       to data.priceStr,
            "requirements" to data.requirements,
            "status"      to "New",
            "imageUrl"    to imageUrl,
            "serviceImageUrl" to imageUrl,
            "freelancerPhotoUrl" to freelancerPhotoUrl,
            "clientPhotoUrl" to clientPhotoUrl,
            "deliveryDays" to service.deliveryTime
        )
    }

    private fun validateForm(data: OrderFormData): Boolean {
        var isValid = true
        
        if (!ValidationUtils.isValidName(data.fullName)) {
            binding.tilFullName.error = ValidationUtils.getNameError(data.fullName)
            isValid = false
        } else {
            binding.tilFullName.error = null
        }

        if (!ValidationUtils.isValidPhone(data.phone)) {
            binding.tilPhone.error = ValidationUtils.getPhoneError(data.phone)
            isValid = false
        } else {
            binding.tilPhone.error = null
        }

        if (!ValidationUtils.isValidEmail(data.email)) {
            binding.tilEmail.error = ValidationUtils.getEmailError(data.email)
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        if (data.requirements.isEmpty()) {
            binding.tilRequirements.error = "Requirements are required"
            isValid = false
        } else {
            binding.tilRequirements.error = null
        }

        return isValid
    }

    private fun startPayment(amountPaise: Long, email: String, phone: String) {
        val checkout = Checkout()
        checkout.setKeyID(getString(R.string.razorpay_key_id))
        try {
            val options = JSONObject().apply {
                put("name",        "Sellora")
                put("description", "Service Order Payment")
                put("theme.color", "#0D5C5C")
                put("currency",    "INR")
                put("amount",      amountPaise)
                put("prefill", JSONObject().apply {
                    put("email",   email)
                    put("contact", phone)
                })
            }
            setLoading(true)
            checkout.open(this, options)
        } catch (e: Exception) {
            setLoading(false)
            Toast.makeText(this, getString(R.string.payment_error, e.message ?: ""), Toast.LENGTH_LONG).show()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        val orderData = currentOrderData ?: return
        orderData["paymentId"] = razorpayPaymentId ?: "N/A"
        viewModel.createOrder(orderData, selectedFileUri)
    }

    override fun onPaymentError(code: Int, response: String?) {
        setLoading(false)
        Log.e("PaymentError", "Code: $code, Response: $response")
        Toast.makeText(this, getString(R.string.payment_failed), Toast.LENGTH_SHORT).show()
    }

    private fun setLoading(isLoading: Boolean, message: String = "Processing...") {
        binding.btnConfirmPayment.isEnabled = !isLoading
        binding.btnConfirmPayment.text      = if (isLoading) message else "Confirm Payment"
    }

    private fun extractPrice(priceStr: String): Long =
        priceStr.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L

    private fun getFileName(uri: Uri): String {
        var name = "Selected file"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
        }
        return name
    }

    private fun copyUriToCache(context: Context, uri: Uri): Uri? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val file = java.io.File(context.cacheDir, "attach_${System.currentTimeMillis()}")
            file.outputStream().use { input.copyTo(it) }
            androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) { null }
    }
}
