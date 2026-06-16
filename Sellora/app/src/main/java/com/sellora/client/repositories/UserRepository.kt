package com.sellora.client.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /** Returns the unique ID of the currently authenticated user. */
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /** Fetches the user profile data from Firestore for the given UID. */
    fun getCurrentUserProfile(uid: String, onResult: (Map<String, Any>?, Exception?) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    onResult(document.data, null)
                } else {
                    onResult(null, Exception("User profile not found"))
                }
            }
            .addOnFailureListener { exception ->
                onResult(null, exception)
            }
    }

    /** Sets up a real-time listener for user profile changes. */
    fun observeUserProfile(uid: String, onUpdate: (Map<String, Any>?) -> Unit): ListenerRegistration {
        return db.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onUpdate(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    onUpdate(snapshot.data)
                } else {
                    onUpdate(null)
                }
            }
    }

    /** Saves or overwrites the entire user profile in Firestore. */
    fun saveUserProfile(uid: String, userProfile: Map<String, Any>, onResult: (Boolean, Exception?) -> Unit) {
        db.collection("users").document(uid)
            .set(userProfile)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { exception ->
                onResult(false, exception)
            }
    }

    /** Updates a specific field in the user profile document. */
    fun updateUserProfileField(uid: String, field: String, value: Any, onResult: (Boolean, Exception?) -> Unit) {
        db.collection("users").document(uid)
            .update(field, value)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it) }
    }
}
