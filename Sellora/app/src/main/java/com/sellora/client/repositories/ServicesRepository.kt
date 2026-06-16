package com.sellora.client.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot

class ServicesRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /** Fetches all active services from Firestore ordered by creation date. */
    fun fetchServices(onResult: (List<DocumentSnapshot>?, Exception?) -> Unit) {
        db.collection("services")
            .whereEqualTo("isActive", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                onResult(documents.documents, null)
            }
            .addOnFailureListener { exception ->
                onResult(null, exception)
            }
    }

    /** Retrieves the name of a partner based on their unique ID. */
    fun fetchPartnerName(partnerId: String, onResult: (String?, Exception?) -> Unit) {
        db.collection("partners").document(partnerId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onResult(doc.getString("name"), null)
                } else {
                    onResult(null, Exception("Partner not found"))
                }
            }
            .addOnFailureListener { exception ->
                onResult(null, exception)
            }
    }
}
