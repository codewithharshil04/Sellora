package com.sellora.client.repositories

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot

class OrdersRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /** Retrieves all orders placed by a specific client. */
    fun getClientOrders(
        clientId: String,
        onComplete: (List<DocumentSnapshot>?, Exception?) -> Unit
    ) {
        db.collection("orders")
            .whereEqualTo("clientId", clientId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { onComplete(it.documents, null) }
            .addOnFailureListener { onComplete(null, it) }
    }

    /** Creates a new order in the Firestore database. */
    fun createOrder(
        orderData: HashMap<String, Any>,
        onComplete: (String?, Exception?) -> Unit
    ) {
        orderData["createdAt"] = FieldValue.serverTimestamp()
        db.collection("orders")
            .add(orderData)
            .addOnSuccessListener { onComplete(it.id, null) }
            .addOnFailureListener { onComplete(null, it) }
    }

    /** Updates the status of an existing order. */
    fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        onComplete: (Boolean, Exception?) -> Unit
    ) {
        db.collection("orders").document(orderId)
            .update("status", newStatus)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { onComplete(false, it) }
    }
}