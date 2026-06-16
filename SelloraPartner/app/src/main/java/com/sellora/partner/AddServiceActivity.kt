package com.sellora.partner

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import coil.decode.VideoFrameDecoder
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.sellora.partner.databinding.ActivityAddServiceBinding
import com.sellora.partner.repositories.MediaRepository
import com.sellora.partner.viewmodels.AddServiceViewModel
import kotlinx.coroutines.launch

class AddServiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddServiceBinding
    private val viewModel: AddServiceViewModel by viewModels()
    private var currentStep       = 1
    private var isEditMode        = false
    private var selectedMediaUri  : Uri?    = null
    private var uploadedMediaUrl  : String? = null
    private var selectedMimeType  : String  = "image/jpeg"

    private val auth               = FirebaseAuth.getInstance()

    private val tierOptions = listOf("Basic", "Adv", "Pro")

    private val tierSpinners: List<Spinner> by lazy {
        listOf(
            binding.spinnerTier1, binding.spinnerTier2, binding.spinnerTier3,
            binding.spinnerTier4, binding.spinnerTier5
        )
    }

    private val mediaPickerLauncher = registerForActivityResult(
        PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedMediaUri = it
            selectedMimeType = contentResolver.getType(it) ?: "image/jpeg"
            uploadedMediaUrl = null

            val isVideo = selectedMimeType.startsWith("video/")
            if (isVideo) {
                // Use Coil to load a video thumbnail
                binding.imgServicePreview.load(it) {
                    decoderFactory { result, options, _ ->
                        VideoFrameDecoder(result.source, options)
                    }
                    placeholder(R.drawable.i_camera)
                    error(R.drawable.i_camera)
                    crossfade(true)
                }
                binding.btnChooseImage.text = getString(R.string.btn_change_video_checked)
            } else {
                binding.imgServicePreview.setImageURI(it)
                binding.btnChooseImage.text = getString(R.string.btn_change_image_checked)
            }
            binding.imgServicePreview.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCategoryDropdown()
        setupTierSpinners()
        setupButtons()
        observeViewModel()
        prefillIfEditMode()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupCategoryDropdown() {
        val cats = listOf("Development", "Design", "Video", "Writing", "Art", "Other")
        binding.actvCategory.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cats)
        )
        binding.actvCategory.setOnClickListener { binding.actvCategory.showDropDown() }
    }

    private fun setupTierSpinners() {
        val spinnerAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, tierOptions
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        tierSpinners.forEach { it.adapter = spinnerAdapter; it.setSelection(0) }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener       { if (currentStep == 2) goToStep(1) else finish() }
        binding.btnChooseImage.setOnClickListener { 
            mediaPickerLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo))
        }
        binding.btnStepBack.setOnClickListener   { if (currentStep == 2) goToStep(1) else finish() }
        binding.btnStepNext.setOnClickListener {
            if (currentStep == 1) { if (validateStep1()) goToStep(2) }
            else                   { if (validateStep2()) uploadMediaThenSave() }
        }
    }

    private fun goToStep(step: Int) {
        currentStep = step
        binding.layoutStep1.visibility    = if (step == 1) View.VISIBLE else View.GONE
        binding.layoutStep2.visibility    = if (step == 2) View.VISIBLE else View.GONE
        updateStepTitle(step)
        binding.txtStepIndicator.text = getString(R.string.step_indicator_format, step)
        binding.btnStepNext.text = when {
            step == 1  -> getString(R.string.btn_continue)
            isEditMode -> getString(R.string.btn_update)
            else       -> getString(R.string.btn_add)
        }
        binding.scrollContent.scrollTo(0, 0)
    }

    private fun updateStepTitle(step: Int) {
        binding.txtStepTitle.text = when {
            step == 1 && isEditMode -> getString(R.string.title_edit_service)
            step == 1               -> getString(R.string.title_add_service)
            step == 2 && isEditMode -> getString(R.string.title_edit_pricing)
            else                    -> getString(R.string.title_pricing_plans)
        }
    }

    private fun prefillIfEditMode() {
        val editId = intent.getStringExtra("edit_service_id") ?: return
        isEditMode = true
        viewModel.fetchService(editId)
    }

    private fun observeViewModel() {
        viewModel.service.observe(this) { service ->
            service?.let { populateFields(it) }
        }

        viewModel.saveStatus.observe(this) { status ->
            when (status) {
                is AddServiceViewModel.SaveStatus.Loading -> {
                    // Show loading if needed
                }
                is AddServiceViewModel.SaveStatus.Uploading -> {
                    binding.btnStepNext.isEnabled = false
                    binding.btnStepNext.text = if (selectedMimeType.startsWith("video/")) {
                        getString(R.string.status_uploading_video)
                    } else {
                        getString(R.string.status_uploading_image)
                    }
                }
                is AddServiceViewModel.SaveStatus.Saving -> {
                    binding.btnStepNext.isEnabled = false
                    binding.btnStepNext.text = getString(R.string.status_saving)
                }
                is AddServiceViewModel.SaveStatus.Success -> {
                    val msg = if (isEditMode) getString(R.string.msg_service_updated) else getString(R.string.msg_service_added)
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is AddServiceViewModel.SaveStatus.Error -> {
                    resetSaveButton()
                    Toast.makeText(this, status.message, Toast.LENGTH_LONG).show()
                }
                is AddServiceViewModel.SaveStatus.Idle -> {
                    resetSaveButton()
                }
            }
        }
    }

    private fun populateFields(service: FreelancerService) {
        binding.txtStepTitle.text = getString(R.string.title_edit_service)
        binding.btnStepNext.text  = getString(R.string.btn_continue)
        binding.etServiceName.setText(service.name)
        binding.etDescription.setText(service.description)
        binding.actvCategory.setText(service.category, false)
        binding.etTime.setText(service.deliveryTime)
        binding.etBasicPrice.setText(service.basicPrice)
        binding.etAdvPrice.setText(service.advPrice)
        binding.etProPrice.setText(service.proPrice)
        
        if (service.imageUri.isNotEmpty()) {
            uploadedMediaUrl = service.imageUri
            binding.imgServicePreview.loadProfileImage(service.imageUri, R.drawable.img_placeholder_service)
            binding.imgServicePreview.visibility = View.VISIBLE
            binding.btnChooseImage.text = getString(R.string.btn_change_media)
        }
        
        val fields = listOf(
            binding.etDeliverable1, binding.etDeliverable2, binding.etDeliverable3,
            binding.etDeliverable4, binding.etDeliverable5
        )
        service.deliverables.forEachIndexed { i, t -> if (i < fields.size) fields[i].setText(t) }
        service.deliverableTiers.forEachIndexed { i, t ->
            if (i < tierSpinners.size) tierSpinners[i].setSelection(tierOptions.indexOf(t).coerceAtLeast(0))
        }
    }

    private fun validateStep1(): Boolean {
        var ok = true
        binding.tilServiceName.error = null
        binding.tilDescription.error = null
        binding.tilCategory.error = null
        
        val name = binding.etServiceName.text.toString().trim()
        val desc = binding.etDescription.text.toString().trim()
        val cat  = binding.actvCategory.text.toString().trim()

        if (!ValidationUtils.isValidServiceName(name)) {
            binding.tilServiceName.error = ValidationUtils.getServiceNameError(name)
            ok = false
        }
        if (desc.isEmpty()) { 
            binding.tilDescription.error = getString(R.string.error_description_required)
            ok = false 
        }
        if (cat.isEmpty())  { 
            binding.tilCategory.error    = getString(R.string.error_category_required)
            ok = false 
        }
        return ok
    }

    private fun validateStep2(): Boolean {
        var ok = true
        binding.tilTime.error = null
        binding.tilBasicPrice.error = null
        binding.tilAdvPrice.error = null
        binding.tilProPrice.error = null
        
        val basic = binding.etBasicPrice.text.toString().trim()
        val adv   = binding.etAdvPrice.text.toString().trim()
        val pro   = binding.etProPrice.text.toString().trim()
        val time  = binding.etTime.text.toString().trim()

        if (!ValidationUtils.isValidPrice(basic)) {
            binding.tilBasicPrice.error = ValidationUtils.getPriceError(basic)
            ok = false
        }
        if (!ValidationUtils.isValidPrice(adv)) {
            binding.tilAdvPrice.error = ValidationUtils.getPriceError(adv)
            ok = false
        }
        if (!ValidationUtils.isValidPrice(pro)) {
            binding.tilProPrice.error = ValidationUtils.getPriceError(pro)
            ok = false
        }
        if (time.isEmpty()) { 
            binding.tilTime.error = getString(R.string.error_delivery_time_required)
            ok = false 
        }
        return ok
    }

    private fun uploadMediaThenSave() {
        val deliverables = mutableListOf<String>()
        val tiers        = mutableListOf<String>()
        collectDeliverables(deliverables, tiers)
        
        val prefs           = getSharedPreferences("sellora_partner_auth", MODE_PRIVATE)
        val partnerId       = auth.currentUser?.uid ?: ""
        val partnerName     = prefs.getString("user_name", "Partner") ?: "Partner"
        val partnerPhotoUrl = prefs.getString("user_profile_pic", "") ?: ""

        viewModel.saveService(
            context = this,
            serviceId = intent.getStringExtra("edit_service_id"),
            partnerId = partnerId,
            partnerName = partnerName,
            partnerPhotoUrl = partnerPhotoUrl,
            name = binding.etServiceName.text.toString().trim(),
            description = binding.etDescription.text.toString().trim(),
            category = binding.actvCategory.text.toString().trim(),
            minPrice = binding.etBasicPrice.text.toString().trim(),
            maxPrice = binding.etProPrice.text.toString().trim(),
            basicPrice = binding.etBasicPrice.text.toString().trim(),
            advPrice = binding.etAdvPrice.text.toString().trim(),
            proPrice = binding.etProPrice.text.toString().trim(),
            deliveryTime = binding.etTime.text.toString().trim(),
            localUri = selectedMediaUri,
            uploadedMediaUrl = uploadedMediaUrl,
            deliverables = deliverables,
            deliverableTiers = tiers
        )
    }

    private fun collectDeliverables(deliverables: MutableList<String>, tiers: MutableList<String>) {
        val fields = listOf(
            binding.etDeliverable1, binding.etDeliverable2, binding.etDeliverable3,
            binding.etDeliverable4, binding.etDeliverable5
        )
        for (i in 0..4) {
            val t = fields[i].text?.toString()?.trim() ?: ""
            if (t.isNotEmpty()) { 
                deliverables.add(t)
                tiers.add(tierSpinners[i].selectedItem?.toString() ?: "Basic") 
            }
        }
    }

    private fun resetSaveButton() {
        binding.btnStepNext.isEnabled = true
        binding.btnStepNext.text      = if (isEditMode) getString(R.string.btn_update) else getString(R.string.btn_add)
    }
}
