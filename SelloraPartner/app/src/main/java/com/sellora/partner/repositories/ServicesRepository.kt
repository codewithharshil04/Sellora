package com.sellora.partner.repositories

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.sellora.partner.FreelancerService
import java.util.Date

class ServicesRepository {
    private val db         = FirebaseFirestore.getInstance()
    private val collection = db.collection("services")

    /**
     * Retrieves all services created by a specific partner, ordered by creation date.
     */
    fun getPartnerServices(partnerId: String): Task<QuerySnapshot> {
        return collection
            .whereEqualTo("partnerId", partnerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
    }

    /**
     * Fetches a single service document by its unique ID.
     */
    fun getServiceById(serviceId: String): Task<DocumentSnapshot> {
        return collection.document(serviceId).get()
    }

    /**
     * Saves a new service or updates an existing one in Firestore.
     */
    fun saveService(
        serviceId       : String?,
        partnerId       : String,
        partnerName     : String,
        partnerPhotoUrl : String,
        name            : String,
        description     : String,
        category        : String,
        minPrice        : String,
        maxPrice        : String,
        basicPrice      : String,
        advPrice        : String,
        proPrice        : String,
        deliveryTime    : String,
        imageUri        : String,
        deliverables    : List<String>,
        deliverableTiers: List<String>
    ): Task<Void> {
        val serviceRef = if (serviceId != null) collection.document(serviceId)
        else                   collection.document()

        val serviceMap = hashMapOf<String, Any?>(
            "id"                 to serviceRef.id,
            "partnerId"          to partnerId,
            "partnerName"        to partnerName,
            "freelancerName"     to partnerName,
            "freelancerPhotoUrl" to partnerPhotoUrl,
            "partnerPhotoUrl"    to partnerPhotoUrl,
            "name"               to name,
            "serviceName"        to name,
            "description"        to description,
            "category"           to category,
            "minPrice"           to minPrice,
            "maxPrice"           to maxPrice,
            "basicPrice"         to basicPrice,
            "advPrice"           to advPrice,
            "proPrice"           to proPrice,
            "deliveryTime"       to deliveryTime,
            "imageUri"           to imageUri,
            "imageUrl"           to imageUri,
            "deliverables"       to deliverables,
            "deliverableTiers"   to deliverableTiers,
            "updatedAt"          to Date()
        )

        return if (serviceId != null) {
            serviceRef.get().continueWithTask { task ->
                val existingIsActive = if (task.isSuccessful) {
                    task.result?.getBoolean("isActive") ?: true
                } else true
                val existingCreatedAt = if (task.isSuccessful) {
                    task.result?.getDate("createdAt") ?: Date()
                } else Date()

                serviceMap["isActive"] = existingIsActive
                serviceMap["createdAt"] = existingCreatedAt
                serviceRef.set(serviceMap)
            }
        } else {
            serviceMap["isActive"] = true
            serviceMap["createdAt"] = Date()
            serviceRef.set(serviceMap)
        }
    }

    /**
     * Deletes a service document from Firestore by its ID.
     */
    fun deleteService(serviceId: String): Task<Void> {
        return collection.document(serviceId).delete()
    }

    /**
     * Maps a Firestore document data map to a FreelancerService object.
     */
    fun mapDocument(id: String, data: Map<String, Any>): FreelancerService {
        val partnerId = data["partnerId"] as? String ?: ""
        val partnerName = data["partnerName"] as? String ?: ""
        val name = data["name"] as? String ?: ""
        val description = data["description"] as? String ?: ""
        val category = data["category"] as? String ?: ""
        
        val basicPrice = data["basicPrice"] as? String ?: ""
        val advPrice = data["advPrice"] as? String ?: ""
        val proPrice = data["proPrice"] as? String ?: ""
        val minPrice = data["minPrice"] as? String ?: ""
        val maxPrice = data["maxPrice"] as? String ?: ""
        
        val deliveryTime = data["deliveryTime"] as? String ?: ""
        val imageUri = data["imageUri"] as? String ?: ""
        val isActive = data["isActive"] as? Boolean ?: true
        
        val deliverables = (data["deliverables"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val deliverableTiers = (data["deliverableTiers"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        return FreelancerService(
            id = id,
            partnerId = partnerId,
            partnerName = partnerName,
            name = name,
            description = description,
            category = category,
            minPrice = minPrice,
            maxPrice = maxPrice,
            basicPrice = basicPrice,
            advPrice = advPrice,
            proPrice = proPrice,
            deliveryTime = deliveryTime,
            imageUri = imageUri,
            deliverables = deliverables,
            deliverableTiers = deliverableTiers,
            isActive = isActive
        )
    }
}
