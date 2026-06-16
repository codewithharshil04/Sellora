package com.sellora.client

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.sellora.client.databinding.ActivityPartnerProfileBinding

class PartnerProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPartnerProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var favoriteIds = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPartnerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val partnerId   = intent.getStringExtra("partner_id")   ?: ""
        val partnerName = intent.getStringExtra("partner_name") ?: ""
        val photoUrl    = intent.getStringExtra("photo_url")    ?: ""

        // Show data we already have instantly — no blank screen while loading
        binding.txtPartnerName.text = partnerName
        if (photoUrl.isNotEmpty()) {
            binding.imgPartnerPhoto.load(photoUrl) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.ic_profile_placeholder)
                error(R.drawable.ic_profile_placeholder)
            }
        }

        binding.btnBack.setOnClickListener { finish() }

        if (partnerId.isEmpty()) {
            Toast.makeText(this, "Partner not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadPartnerProfile(partnerId)
        loadPartnerServices(partnerId)
        loadOtherPartners(partnerId)
        loadUserFavorites()
    }

    private fun loadPartnerProfile(partnerId: String) {
        db.collection("partners").document(partnerId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                binding.txtPartnerName.text     = doc.getString("name") ?: ""
                binding.txtUsername.text        = "@${doc.getString("username") ?: doc.getString("name")?.lowercase()?.replace(" ", "") ?: ""}"
                val bio = doc.getString("bio") ?: doc.getString("description") ?: ""
                if (bio.isNotEmpty()) {
                    binding.txtBio.text       = bio
                    binding.txtBio.visibility = View.VISIBLE
                    
                    // Handle "more" button for bio
                    binding.txtBio.post {
                        val layout = binding.txtBio.layout
                        if (layout != null) {
                            val lines = layout.lineCount
                            if (lines > 0) {
                                // Check if the text is ellipsized on the last visible line
                                if (layout.getEllipsisCount(lines - 1) > 0) {
                                    binding.txtMoreBio.visibility = View.VISIBLE
                                }
                            }
                        }
                    }
                    
                    binding.txtMoreBio.setOnClickListener {
                        if (binding.txtBio.maxLines == 3) {
                            binding.txtBio.maxLines = 100
                            binding.txtMoreBio.text = "less"
                        } else {
                            binding.txtBio.maxLines = 3
                            binding.txtMoreBio.text = "more"
                        }
                    }
                } else {
                    binding.txtBio.visibility = View.GONE
                    binding.txtMoreBio.visibility = View.GONE
                }

                val photo = doc.getString("photoUrl") ?: doc.getString("profilePicUrl") ?: ""
                if (photo.isNotEmpty()) {
                    binding.imgPartnerPhoto.load(photo) {
                        crossfade(true)
                        memoryCacheKey("partner_pfp_$partnerId")
                        transformations(CircleCropTransformation())
                        placeholder(R.drawable.ic_profile_placeholder)
                        error(R.drawable.ic_profile_placeholder)
                    }
                }
            }
    }

    private fun loadPartnerServices(partnerId: String) {
        binding.servicesProgress.visibility = View.VISIBLE
        db.collection("services")
            .whereEqualTo("partnerId", partnerId)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { snap ->
                binding.servicesProgress.visibility = View.GONE
                val services = snap.documents.mapNotNull { doc ->
                    try {
                        val min = doc.get("minPrice")?.toString() ?: ""
                        val max = doc.get("maxPrice")?.toString() ?: ""
                        val price = if (min.isNotEmpty() && max.isNotEmpty()) "₹$min–₹$max" else "Price on request"
                        val deliverables     = (doc.get("deliverables") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        val deliverableTiers = (doc.get("deliverableTiers") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        Service(
                            id                 = doc.id,
                            imageUrl           = doc.getString("imageUrl") ?: doc.getString("imageUri") ?: "",
                            freelancerName     = doc.getString("partnerName") ?: doc.getString("freelancerName") ?: "",
                            freelancerPhotoUrl = doc.getString("freelancerPhotoUrl") ?: doc.getString("partnerPhotoUrl") ?: "",
                            priceRange         = price,
                            serviceName        = doc.getString("serviceName") ?: doc.getString("name") ?: "",
                            partnerId          = doc.getString("partnerId") ?: "",
                            category           = doc.getString("category") ?: "",
                            description        = doc.getString("description") ?: "",
                            basicPrice         = doc.getString("basicPrice") ?: "₹0",
                            advPrice           = doc.getString("advPrice")   ?: "₹0",
                            proPrice           = doc.getString("proPrice")   ?: "₹0",
                            deliverables       = deliverables,
                            deliverableTiers   = deliverableTiers,
                            deliveryTime       = doc.getString("deliveryTime") ?: ""
                        )
                    } catch (e: Exception) { null }
                }
                if (services.isNotEmpty()) {
                    binding.txtServicesLabel.visibility = View.VISIBLE
                    binding.rvServices.visibility       = View.VISIBLE
                    binding.rvServices.layoutManager    = LinearLayoutManager(this)
                    binding.rvServices.adapter          = ServiceAdapter(favoriteIds) { serviceId ->
                        toggleFavorite(serviceId)
                    }.apply { submitList(services) }
                } else {
                    binding.txtServicesLabel.visibility = View.GONE
                    binding.rvServices.visibility       = View.GONE
                }
            }
            .addOnFailureListener { binding.servicesProgress.visibility = View.GONE }
    }

    private fun loadUserFavorites() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val list = (doc.get("favoriteServiceIds") as? List<*>)?.filterIsInstance<String>()
                favoriteIds = list?.toMutableSet() ?: mutableSetOf()
                (binding.rvServices.adapter as? ServiceAdapter)?.updateFavorites(favoriteIds)
            }
    }

    private fun toggleFavorite(serviceId: String) {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Please login to favorite services", Toast.LENGTH_SHORT).show()
            return
        }

        val isCurrentlyFavorite = serviceId in favoriteIds
        if (isCurrentlyFavorite) favoriteIds.remove(serviceId) else favoriteIds.add(serviceId)
        (binding.rvServices.adapter as? ServiceAdapter)?.updateFavorites(favoriteIds.toSet())

        val operation = if (isCurrentlyFavorite)
            FieldValue.arrayRemove(serviceId)
        else
            FieldValue.arrayUnion(serviceId)

        db.collection("users").document(userId)
            .update("favoriteServiceIds", operation)
            .addOnFailureListener {
                // Rollback
                if (isCurrentlyFavorite) favoriteIds.add(serviceId) else favoriteIds.remove(serviceId)
                (binding.rvServices.adapter as? ServiceAdapter)?.updateFavorites(favoriteIds.toSet())
                Toast.makeText(this, "Failed to update favorite", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadOtherPartners(currentPartnerId: String) {
        db.collection("partners").limit(10).get()
            .addOnSuccessListener { snap ->
                val others = snap.documents.filter { it.id != currentPartnerId }
                if (others.isEmpty()) return@addOnSuccessListener

                binding.txtOtherPartnersLabel.visibility = View.VISIBLE
                binding.rvOtherPartners.visibility       = View.VISIBLE
                binding.rvOtherPartners.layoutManager    =
                    LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                binding.rvOtherPartners.adapter          = OtherPartnerAdapter(others) { doc ->
                    startActivity(Intent(this, PartnerProfileActivity::class.java).apply {
                        putExtra("partner_id",   doc.id)
                        putExtra("partner_name", doc.getString("name") ?: "")
                        putExtra("photo_url",    doc.getString("photoUrl") ?: "")
                    })
                }
            }
    }
}

class OtherPartnerAdapter(
    private val partners: List<com.google.firebase.firestore.DocumentSnapshot>,
    private val onClick: (com.google.firebase.firestore.DocumentSnapshot) -> Unit
) : RecyclerView.Adapter<OtherPartnerAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val img  : ImageView = view.findViewById(R.id.imgOtherPartner)
        val name : TextView  = view.findViewById(R.id.txtOtherPartnerName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_other_partner, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val doc   = partners[position]
        val photo = doc.getString("photoUrl") ?: ""
        val name  = doc.getString("name") ?: ""
        holder.name.text = name
        holder.img.load(photo) {
            crossfade(true)
            memoryCacheKey("other_partner_${doc.id}")
            transformations(CircleCropTransformation())
            placeholder(R.drawable.ic_profile_placeholder)
            error(R.drawable.ic_profile_placeholder)
        }
        holder.itemView.setOnClickListener { onClick(doc) }
        MotionUtils.applyPressEffect(holder.itemView)
    }

    override fun getItemCount() = partners.size
}
