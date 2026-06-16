package com.sellora.partner.repositories

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class PartnerRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("partners")

    /**
     * Retrieves the profile data for a partner from Firestore using their unique UID.
     */
    fun getPartnerProfile(uid: String): Task<DocumentSnapshot> {
        return collection.document(uid).get()
    }

    /**
     * Saves or updates the partner's profile information in the Firestore database.
     */
    fun savePartnerProfile(
        uid: String,
        username: String,
        name: String,
        phone: String,
        email: String,
        pan: String,
        photoUrl: String? = null
    ): Task<Void> {
        val userMap = mutableMapOf<String, Any>(
            "uid" to uid,
            "username" to username,
            "name" to name,
            "phone" to phone,
            "email" to email,
            "pan" to pan,
            "role" to "partner"
        )
        photoUrl?.let { userMap["photoUrl"] = it }
        return collection.document(uid).set(userMap, com.google.firebase.firestore.SetOptions.merge())
    }
}
