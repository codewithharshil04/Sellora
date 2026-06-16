package com.sellora.client

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sellora.client.databinding.ActivitySavedServicesBinding

class SavedServicesActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedServicesBinding
    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: ServiceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedServicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            binding.txtEmpty.text       = "Please login to see saved services"
            binding.txtEmpty.visibility = View.VISIBLE
            binding.rvSavedServices.visibility = View.GONE
            return
        }

        binding.rvSavedServices.layoutManager = LinearLayoutManager(this)
        
        // Disable change animations to prevent flicker
        (binding.rvSavedServices.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val savedIds = (userDoc.get("favoriteServiceIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val favoriteSet = savedIds.toSet()

                if (savedIds.isEmpty()) {
                    binding.txtEmpty.visibility        = View.VISIBLE
                    binding.rvSavedServices.visibility = View.GONE
                    return@addOnSuccessListener
                }

                db.collection("services")
                    .whereIn("__name__", savedIds.take(30))
                    .get()
                    .addOnSuccessListener { documents ->
                        val services = documents.mapNotNull { doc ->
                            try {
                                val minPrice   = doc.get("minPrice")?.toString()
                                val maxPrice   = doc.get("maxPrice")?.toString()
                                val priceRange = if (minPrice != null && maxPrice != null)
                                    "₹$minPrice–₹$maxPrice"
                                else doc.getString("priceRange") ?: "Price on request"

                                val imageUrl = doc.getString("imageUrl")
                                    ?: doc.getString("image")
                                    ?: ""

                                val freelancerPhotoUrl = doc.getString("freelancerPhotoUrl")
                                    ?: doc.getString("partnerPhotoUrl")
                                    ?: ""

                                val deliverables = (doc.get("deliverables") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                                val deliverableTiers = (doc.get("deliverableTiers") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                                Service(
                                    id                 = doc.id,
                                    imageUrl           = imageUrl,
                                    freelancerName     = doc.getString("freelancerName")
                                        ?: doc.getString("partnerName") ?: "Freelancer",
                                    freelancerPhotoUrl = freelancerPhotoUrl,
                                    priceRange         = priceRange,
                                    serviceName        = doc.getString("serviceName")
                                        ?: doc.getString("name") ?: "Service",
                                    partnerId          = doc.getString("partnerId") ?: "",
                                    category           = doc.getString("category") ?: "Other",
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

                        if (services.isEmpty()) {
                            binding.txtEmpty.visibility        = View.VISIBLE
                            binding.rvSavedServices.visibility = View.GONE
                        } else {
                            binding.txtEmpty.visibility        = View.GONE
                            binding.rvSavedServices.visibility = View.VISIBLE
                            
                            // Initialize adapter with favoriteSet and submit list
                            adapter = ServiceAdapter(favoriteSet) { serviceId ->
                                // Logic to remove from favorites and update list
                                toggleFavoriteInSaved(serviceId)
                            }
                            binding.rvSavedServices.adapter = adapter
                            adapter.submitList(services)
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleFavoriteInSaved(serviceId: String) {
        val userId = auth.currentUser?.uid ?: return
        
        // Optimistic UI Update
        val currentList = adapter.currentList.toMutableList()
        val index = currentList.indexOfFirst { it.id == serviceId }
        
        // In SavedServicesActivity, clicking heart usually means "unsave"
        // For simplicity, we just call the firestore update and let the user refresh or handle it
        // But since we want it to "work correctly", let's handle the removal.

        db.collection("users").document(userId)
            .update("favoriteServiceIds", com.google.firebase.firestore.FieldValue.arrayRemove(serviceId))
            .addOnSuccessListener {
                if (index != -1) {
                    currentList.removeAt(index)
                    adapter.submitList(currentList)
                    if (currentList.isEmpty()) {
                        binding.txtEmpty.visibility = View.VISIBLE
                        binding.rvSavedServices.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show()
            }
    }
}
