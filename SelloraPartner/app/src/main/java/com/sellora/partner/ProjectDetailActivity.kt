package com.sellora.partner

import com.sellora.partner.SupabaseDownloader
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.OpenableColumns
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.sellora.partner.databinding.ActivityProjectDetailBinding
import com.sellora.partner.viewmodels.ProjectDetailViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class ProjectDetailActivity : AppCompatActivity() {

    private var selectedFileUri: Uri? = null
    private lateinit var binding: ActivityProjectDetailBinding
    private val viewModel: ProjectDetailViewModel by viewModels()
    private var currentOrderId = ""
    private var countDownTimer: CountDownTimer? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            val fileName = getFileName(it)
            binding.btnUploadFile.text = getString(R.string.btn_change_file)
            binding.txtUploadedFileName.text = "📎 $fileName"
            binding.txtUploadedFileName.visibility = View.VISIBLE
            binding.btnMarkDelivered.isEnabled = true
            binding.btnMarkDelivered.alpha = 1f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentOrderId = intent.getStringExtra("order_id")
            ?: intent.getStringExtra("project_id") ?: ""

        // Load initial data from Intent for immediate UI feedback
        val serviceName = intent.getStringExtra("service_name") ?: "Loading..."
        val clientName  = intent.getStringExtra("client_name")  ?: "Client"
        val date        = intent.getStringExtra("date")         ?: "—"
        val price       = intent.getStringExtra("price")        ?: "—"
        val status      = intent.getStringExtra("status")       ?: "New"
        val timer       = intent.getStringExtra("timer")        ?: "00:00:00"
        val imageUrl    = intent.getStringExtra("image_url")
        val requirements = intent.getStringExtra("requirements")
        val reqFileUrl  = intent.getStringExtra("requirementFileUrl") ?: intent.getStringExtra("fileUrl")
        val deliveryUrl = intent.getStringExtra("deliveryFileUrl")
        val deliveryDays = intent.getLongExtra("delivery_days", 0L)
        val createdAt    = intent.getLongExtra("created_at", 0L)

        setupUI(
            serviceName = serviceName,
            clientName = clientName,
            date = date,
            price = price,
            status = status,
            timer = timer,
            imageUrl = imageUrl,
            requirements = requirements,
            requirementFileUrl = reqFileUrl,
            deliveryFileUrl = deliveryUrl
        )

        // Start timer immediately if we have the data
        if (createdAt > 0 && deliveryDays > 0) {
            val deadlineMs = createdAt + deliveryDays * 24 * 60 * 60 * 1000L
            startCountdown(deadlineMs)
        }

        if (currentOrderId.isNotEmpty()) {
            viewModel.startOrderListener(currentOrderId)
        }
        observeViewModel()

        binding.btnUploadFile.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        binding.btnMarkDelivered.setOnClickListener {
            if (selectedFileUri == null) {
                Toast.makeText(this, getString(R.string.error_upload_file_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_title_complete_delivery))
                .setMessage(getString(R.string.dialog_msg_complete_delivery, binding.txtServiceName.text))
                .setPositiveButton(getString(R.string.btn_confirm_delivery)) { _, _ -> performDelivery() }
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show()
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        viewModel.project.observe(this) { project ->
            project?.let {
                setupUI(
                    serviceName = it.serviceName,
                    clientName  = it.freelancerName,
                    date        = it.date,
                    price       = it.price,
                    status      = it.status,
                    timer       = it.timer,
                    imageUrl    = it.serviceImageUrl,
                    requirements = it.requirements,
                    requirementFileUrl = it.requirementFileUrl,
                    deliveryFileUrl = it.deliveryFileUrl
                )

                if (it.createdAt > 0 && it.deliveryDays > 0) {
                    val deadlineMs = it.createdAt + it.deliveryDays * 24 * 60 * 60 * 1000L
                    startCountdown(deadlineMs)
                } else {
                    binding.txtTimer.text = "—"
                }
            }
        }

        viewModel.uploadStatus.observe(this) { status ->
            when (status) {
                is ProjectDetailViewModel.UploadStatus.Uploading -> {
                    binding.btnMarkDelivered.isEnabled = false
                    binding.btnMarkDelivered.text      = getString(R.string.status_uploading_file)
                }
                is ProjectDetailViewModel.UploadStatus.Saving -> {
                    binding.btnMarkDelivered.text = getString(R.string.status_saving)
                }
                is ProjectDetailViewModel.UploadStatus.Success -> {
                    Toast.makeText(this, getString(R.string.msg_delivery_success), Toast.LENGTH_LONG).show()
                    finish()
                }
                is ProjectDetailViewModel.UploadStatus.Error -> {
                    binding.btnMarkDelivered.isEnabled = true
                    binding.btnMarkDelivered.text      = getString(R.string.btn_complete_delivery)
                    Toast.makeText(this, status.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun startCountdown(deadlineMs: Long) {
        countDownTimer?.cancel()
        val remaining = deadlineMs - System.currentTimeMillis()
        if (remaining <= 0) {
            binding.txtTimer.text = getString(R.string.status_overdue)
            return
        }
        countDownTimer = object : CountDownTimer(remaining, 1000) {
            override fun onTick(ms: Long) {
                val d = ms / (1000 * 60 * 60 * 24)
                val h = (ms / (1000 * 60 * 60)) % 24
                val m = (ms / (1000 * 60)) % 60
                val s = (ms / 1000) % 60
                binding.txtTimer.text = if (d > 0) "${d}d ${h}h ${m}m" else String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
            }

            override fun onFinish() {
                binding.txtTimer.text = getString(R.string.status_overdue)
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    private fun setupUI(
        serviceName: String, clientName: String, date: String,
        price: String, status: String, timer: String, imageUrl: String?,
        requirements: String?, requirementFileUrl: String?, deliveryFileUrl: String?
    ) {
        bindOrderData(serviceName, clientName, date, price, timer, status, imageUrl, requirements, requirementFileUrl)
        applyStatusStyle(status, !requirementFileUrl.isNullOrEmpty(), deliveryFileUrl)
    }

    private fun bindOrderData(
        serviceName: String, clientName: String, date: String,
        price: String, timer: String, status: String, imageUrl: String?,
        requirements: String?, requirementFileUrl: String?
    ) {
        binding.txtServiceName.text = serviceName
        binding.txtClientName.text  = clientName
        binding.txtDate.text        = date
        binding.txtPrice.text       = if (price.startsWith("₹")) price else "₹$price"
        binding.txtTimer.text       = timer
        binding.txtStatus.text      = status

        // Requirement logic centralized
        if (requirements.isNullOrBlank() && requirementFileUrl.isNullOrEmpty()) {
            binding.cardRequirementFile.visibility = View.GONE
        } else {
            binding.cardRequirementFile.visibility = View.VISIBLE
            
            // Extract actual filename from URL for better UI
            val fileNameDisplay = if (!requirementFileUrl.isNullOrEmpty()) {
                val uri = Uri.parse(requirementFileUrl.substringBefore('?'))
                uri.lastPathSegment ?: "Requirement File"
            } else {
                getString(R.string.requirements_attached)
            }

            binding.txtRequirementFileName.text = when {
                !requirements.isNullOrBlank() -> requirements
                !requirementFileUrl.isNullOrEmpty() -> fileNameDisplay
                else -> "No requirements provided"
            }
            
            if (!requirementFileUrl.isNullOrEmpty()) {
                binding.btnDownloadRequirement.visibility = View.VISIBLE
                val cleanUrl = requirementFileUrl.substringBefore('?')
                val ext = cleanUrl.substringAfterLast('.', "").lowercase()
                
                binding.btnDownloadRequirement.text = when (ext) {
                    "zip", "rar" -> "Download ZIP"
                    "mp4", "mkv", "mov" -> "Open Video"
                    "pdf" -> "Open PDF"
                    else -> "Download File"
                }

                binding.btnDownloadRequirement.setOnClickListener {
                    openUrl(requirementFileUrl)
                }
            } else {
                binding.btnDownloadRequirement.visibility = View.GONE
            }
        }

        // Use robust image loader
        binding.imgService.loadProfileImage(imageUrl, R.drawable.img_placeholder_service)
    }

    private fun applyStatusStyle(status: String, hasRequirementFile: Boolean, deliveryFileUrl: String?) {
        val badgeColor = when (status) {
            "New", "Pending" -> "#F59E0B"
            "In Progress"    -> "#0D5C5C"
            "Delivered"      -> "#22C55E"
            "Cancelled"      -> "#EF4444"
            else             -> "#64748B"
        }
        binding.txtStatus.text = status
        binding.txtStatus.background?.setTint(Color.parseColor(badgeColor))
        binding.txtStatus.setTextColor(
            if (status == "New" || status == "Pending") Color.BLACK else Color.WHITE
        )

        when (status) {
            "New", "Pending", "In Progress" -> {
                binding.btnUploadFile.visibility    = View.VISIBLE
                binding.btnMarkDelivered.visibility = View.VISIBLE
                binding.btnViewDelivery.visibility  = View.GONE
                binding.txtUploadedFileName.visibility = if (selectedFileUri != null) View.VISIBLE else View.GONE
                
                if (selectedFileUri == null) {
                    binding.btnMarkDelivered.isEnabled = false
                    binding.btnMarkDelivered.alpha     = 0.6f
                } else {
                    binding.btnMarkDelivered.isEnabled = true
                    binding.btnMarkDelivered.alpha     = 1.0f
                }
            }
            "Delivered" -> {
                binding.btnMarkDelivered.visibility    = View.GONE
                binding.btnUploadFile.visibility       = View.GONE
                binding.txtUploadedFileName.visibility = View.GONE
                
                if (!deliveryFileUrl.isNullOrEmpty()) {
                    binding.btnViewDelivery.visibility = View.VISIBLE
                    val cleanUrl = deliveryFileUrl.substringBefore('?')
                    val extension = cleanUrl.substringAfterLast('.', "").lowercase()
                    binding.btnViewDelivery.text = if (extension == "zip") "Download ZIP Delivery" else "View Delivery (.$extension)"

                    binding.btnViewDelivery.setOnClickListener { 
                        openUrl(deliveryFileUrl)
                    }
                } else {
                    binding.btnViewDelivery.visibility = View.GONE
                }
            }
            else -> {
                binding.btnMarkDelivered.visibility    = View.GONE
                binding.btnUploadFile.visibility       = View.GONE
                binding.txtUploadedFileName.visibility = View.GONE
                binding.btnViewDelivery.visibility     = View.GONE
            }
        }
        // Always allow requirement download if it exists
        binding.btnDownloadRequirement.visibility = if (hasRequirementFile) View.VISIBLE else View.GONE
    }

    private fun performDelivery() {
        val uri     = selectedFileUri ?: return
        val orderId = currentOrderId.ifEmpty { return }
        viewModel.performDelivery(this, orderId, uri)
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = it.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: getString(R.string.file_selected)
    }

    private fun openUrl(url: String) {
        try {
            val cleanUrl = url.substringBefore('?')
            val extension = cleanUrl.substringAfterLast('.', "").lowercase()
            val fileName = Uri.parse(cleanUrl).lastPathSegment ?: "file.$extension"
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                ?: "application/octet-stream"

            // 1. For Images, show in-app preview
            if (extension == "jpg" || extension == "jpeg" || extension == "png" || extension == "webp") {
                showImagePreview(url)
                return
            }

            // 2. For EVERYTHING ELSE, use DownloadManager (keeps name & avoids .bin)
            Toast.makeText(this, "Downloading $fileName...", Toast.LENGTH_SHORT).show()

            val request = android.app.DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("Downloading...")
                .setMimeType(mimeType)
                .addRequestHeader("Content-Disposition", "attachment; filename=\"$fileName\"")
                .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            downloadManager.enqueue(request)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImagePreview(url: String) {
        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .create()
        val imageView = android.widget.ImageView(this)
        imageView.layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        imageView.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        imageView.setBackgroundColor(Color.BLACK)
        
        // Use Coil to load the image
        imageView.load(url) {
            placeholder(R.drawable.img_placeholder_service)
            error(R.drawable.img_placeholder_service)
        }
        
        imageView.setOnClickListener { dialog.dismiss() }
        dialog.setView(imageView)
        dialog.show()
    }

}
