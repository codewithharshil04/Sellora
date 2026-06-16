package com.sellora.client

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.sellora.client.databinding.ActivityServiceDetailBinding
import com.google.gson.Gson
import com.sellora.client.viewmodels.ServiceDetailViewModel

class ServiceDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServiceDetailBinding
    private lateinit var deliverableAdapter: DeliverableAdapter
    private val viewModel: ServiceDetailViewModel by viewModels()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServiceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val serviceJson = intent.getStringExtra("service_json")
        val service = if (serviceJson != null) gson.fromJson(serviceJson, Service::class.java) else Service()

        setupUI(service, serviceJson)
        setupObservers(service.id, service.category)
    }

    private fun setupUI(service: Service, serviceJson: String?) {
        binding.txtServiceName.text    = service.serviceName
        binding.txtFreelancerName.text = service.freelancerName
        binding.txtCategoryTag.text    = service.category

        val openPartnerProfile = View.OnClickListener {
            if (service.partnerId.isNotEmpty()) {
                startActivity(Intent(this, PartnerProfileActivity::class.java).apply {
                    putExtra("partner_id",   service.partnerId)
                    putExtra("partner_name", service.freelancerName)
                    putExtra("photo_url",    service.freelancerPhotoUrl)
                })
            }
        }
        binding.imgFreelancerDetail.setOnClickListener(openPartnerProfile)
        binding.txtFreelancerName.setOnClickListener(openPartnerProfile)

        if (service.freelancerPhotoUrl.isNotEmpty()) {
            binding.imgFreelancerDetail.loadImage(service.freelancerPhotoUrl, R.drawable.ic_profile_placeholder)
        }
        binding.txtDescription.text    = service.description
        
        binding.txtBasicPrice.text = service.basicPrice
        binding.txtAdvPrice.text   = service.advPrice
        binding.txtProPrice.text   = service.proPrice

        binding.imgService.loadImage(service.imageUrl, R.drawable.img_placeholder_service)

        binding.btnBack.setOnClickListener { finish() }

        val deliverables = if (service.deliverables.isNotEmpty()) {
            service.deliverables.mapIndexed { i, name ->
                Deliverable(name = name, tier = service.deliverableTiers.getOrElse(i) { "Basic" })
            }
        } else {
            listOf(Deliverable("Details provided after order", "Basic"))
        }

        binding.rvDeliverables.layoutManager = LinearLayoutManager(this)
        deliverableAdapter = DeliverableAdapter(deliverables, "Basic")
        binding.rvDeliverables.adapter = deliverableAdapter

        binding.txtMore.setOnClickListener {
            if (binding.txtDescription.maxLines == 3) {
                binding.txtDescription.maxLines = 100
                binding.txtMore.text = "less"
            } else {
                binding.txtDescription.maxLines = 3
                binding.txtMore.text = "more"
            }
        }

        var selectedPlan = "Basic"
        binding.togglePlan.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedPlan = when (checkedId) {
                R.id.btnBasic -> "Basic"
                R.id.btnAdv   -> "Adv"
                R.id.btnPro   -> "Pro"
                else          -> "Basic"
            }
            deliverableAdapter.setActivePlan(selectedPlan)
        }

        binding.btnContinue.setOnClickListener {
            startActivity(
                Intent(this, PlaceOrderActivity::class.java).apply {
                    putExtra("service_json",  serviceJson)
                    putExtra("selected_plan", selectedPlan)
                }
            )
        }
    }

    private fun setupObservers(serviceId: String, category: String) {
        viewModel.recommendedServices.observe(this) { recommended ->
            if (recommended.isNotEmpty()) {
                binding.rvRecommended.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                binding.rvRecommended.adapter = ServiceAdapter(emptySet()) { id ->
                    // For now, recommendation favoriting is not wired to a VM here, or can be if needed.
                    // But we must provide the lambda to satisfy the new constructor.
                }.apply { submitList(recommended) }
                binding.labelRecommended.visibility = View.VISIBLE
                binding.rvRecommended.visibility = View.VISIBLE
            }
        }
        viewModel.fetchRecommendedServices(category, serviceId)
    }
}
