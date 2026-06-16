package com.sellora.client

import android.content.Context
import com.sellora.client.SupabaseDownloader
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.sellora.client.databinding.ActivityProjectDetailBinding
import com.sellora.client.viewmodels.ProjectDetailViewModel
import kotlinx.coroutines.launch

class ProjectDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProjectDetailBinding
    private val viewModel: ProjectDetailViewModel by viewModels()
    private var countDownTimer: CountDownTimer? = null
    private var serviceName: String = "Service"
    private var projectId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        projectId          = intent.getStringExtra("project_id") ?: ""
        serviceName        = intent.getStringExtra("service_name") ?: "Service"
        val freelancerName = intent.getStringExtra("freelancer_name") ?: "Freelancer"
        val date           = intent.getStringExtra("date") ?: "—"
        val price          = intent.getStringExtra("price") ?: "—"
        val imageUrl       = intent.getStringExtra("image_url") ?: ""

        binding.txtServiceName.text    = serviceName
        binding.txtFreelancerName.text = freelancerName
        binding.txtDate.text           = date
        binding.txtPrice.text          = price
        binding.txtTimer.text          = "Loading…"

        if (imageUrl.isNotEmpty()) binding.imgService.loadImage(imageUrl, R.drawable.img_placeholder_service)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnContactAdmin.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:admin@sellora.com")
                putExtra(Intent.EXTRA_SUBJECT, "Support: Project $projectId")
                putExtra(Intent.EXTRA_TEXT, "Hello Sellora Team,\n\nI need help with my project ($serviceName).\nOrder ID: $projectId\n\n[Describe your issue here]")
            }
            try {
                startActivity(Intent.createChooser(intent, "Contact Admin"))
            } catch (e: Exception) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }

        setupObservers()
        setupListeners()

        if (projectId.isNotEmpty()) {
            viewModel.fetchOrderDetails(projectId) // Initial load
            viewModel.startOrderListener(projectId) // Real-time updates
        }
    }

    private fun setupObservers() {
        viewModel.orderDetails.observe(this) { data ->
            if (data != null) {
                val status             = data["status"] as? String ?: "New"
                val requirements       = data["requirements"] as? String ?: ""
                val deliveryFileUrl    = data["deliveryFileUrl"] as? String ?: ""
                val requirementFileUrl = (data["requirementFileUrl"] ?: data["fileUrl"]) as? String ?: ""
                val deliveryDays       = (data["deliveryDays"]?.toString())?.toLongOrNull() ?: 0L
                val createdAt          = data["createdAt"] as? com.google.firebase.Timestamp

                if (requirements.isNotEmpty()) {
                    binding.txtRequirementText.text       = requirements
                    binding.txtRequirementText.visibility = View.VISIBLE
                }

                if (createdAt != null && deliveryDays > 0) {
                    val deadlineMs = createdAt.toDate().time + deliveryDays * 24 * 60 * 60 * 1000L
                    startCountdown(deadlineMs)
                } else {
                    binding.txtTimer.text = "—"
                }

                applyStatus(status, deliveryFileUrl, requirementFileUrl)
            }
        }

        viewModel.cancelResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Order cancelled", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                binding.btnCancel.isEnabled = true
                Toast.makeText(this, "Failed to cancel order", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cancel Order")
                .setMessage("Cancel \"$serviceName\"?")
                .setPositiveButton("Yes, Cancel") { _, _ ->
                    binding.btnCancel.isEnabled = false
                    viewModel.cancelOrder(projectId)
                }
                .setNegativeButton("No", null).show()
        }
    }

    private fun startCountdown(deadlineMs: Long) {
        countDownTimer?.cancel()
        val remaining = deadlineMs - System.currentTimeMillis()
        if (remaining <= 0) { binding.txtTimer.text = "Overdue"; return }

        countDownTimer = object : CountDownTimer(remaining, 1000) {
            override fun onTick(ms: Long) {
                val d  = ms / (1000 * 60 * 60 * 24)
                val h  = (ms / (1000 * 60 * 60)) % 24
                val m  = (ms / (1000 * 60)) % 60
                val s  = (ms / 1000) % 60
                binding.txtTimer.text = if (d > 0) "${d}d ${h}h ${m}m" else "%02d:%02d:%02d".format(h, m, s)
            }
            override fun onFinish() { binding.txtTimer.text = "Overdue" }
        }.start()
    }

    private fun applyStatus(status: String, deliveryFileUrl: String, requirementFileUrl: String) {
        binding.txtStatus.text = status
        val badgeColor = when (status) {
            "New" -> "#F59E0B"
            "Delivered"      -> "#4CAF50"
            "Cancelled"      -> "#EF4444"
            else             -> "#888888"
        }
        binding.txtStatus.background?.setTint(Color.parseColor(badgeColor))
        binding.txtStatus.setTextColor(
            if (status == "New") Color.parseColor("#1A1A1A") else Color.WHITE
        )

        // Always show requirement card if a file exists
        if (requirementFileUrl.isNotEmpty()) {
            binding.cardRequirementFile.visibility = View.VISIBLE
            
            // Extract actual filename from URL
            val uri = Uri.parse(requirementFileUrl.substringBefore('?'))
            val fileName = uri.lastPathSegment ?: "Requirement File"
            binding.txtRequirementFileName.text = fileName
            
            setupRequirementDownload(requirementFileUrl, serviceName, projectId)
        } else {
            binding.cardRequirementFile.visibility = View.GONE
        }

        when (status) {
            "New" -> {
                binding.cardDeliveredFile.visibility = View.GONE
                binding.btnCancel.visibility         = View.VISIBLE
            }
            "Delivered" -> {
                binding.btnCancel.visibility         = View.GONE
                binding.cardDeliveredFile.visibility = View.VISIBLE
                
                // Extract actual filename from URL
                val uri = Uri.parse(deliveryFileUrl.substringBefore('?'))
                val fileName = uri.lastPathSegment ?: "Delivery File"
                binding.txtFileName.text = fileName

                binding.btnDownload.visibility       = View.VISIBLE
                binding.btnDownload.setOnClickListener {
                    if (deliveryFileUrl.isEmpty()) {
                        Toast.makeText(this, "File not available yet", Toast.LENGTH_SHORT).show()
                    } else {
                        openFile(deliveryFileUrl)
                    }
                }
            }
            "Cancelled" -> {
                binding.cardDeliveredFile.visibility = View.GONE
                binding.btnCancel.visibility         = View.GONE
            }
            else -> {
                binding.cardDeliveredFile.visibility = View.GONE
                binding.btnCancel.visibility         = View.GONE
            }
        }
    }

    private fun setupRequirementDownload(url: String, serviceName: String, orderId: String) {
        if (url.isEmpty()) {
            binding.btnDownloadRequirement.visibility = View.GONE
        } else {
            binding.btnDownloadRequirement.visibility = View.VISIBLE
            binding.btnDownloadRequirement.setOnClickListener {
                openFile(url)
            }
        }
    }

    private fun openFile(url: String) {
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

            // 2. For EVERYTHING ELSE, use DownloadManager (Reliable & keeps name)
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

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            downloadManager.enqueue(request)
            
        } catch (e: Exception) {
            Log.e("ProjectDetail", "Download error", e)
            Toast.makeText(this, "Could not download file", Toast.LENGTH_SHORT).show()
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
        
        // Use Coil to load image
        imageView.load(url) {
            placeholder(R.drawable.img_placeholder_service)
            error(R.drawable.img_placeholder_service)
        }
        
        imageView.setOnClickListener { dialog.dismiss() }
        dialog.setView(imageView)
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
