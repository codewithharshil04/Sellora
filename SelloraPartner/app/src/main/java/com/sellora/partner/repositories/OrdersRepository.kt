package com.sellora.partner.repositories

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class OrdersRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("orders")

    /**
     * Fetches all orders associated with a specific partner ID from Firestore.
     */
    fun fetchOrdersByPartner(partnerId: String): Task<QuerySnapshot> {
        return collection
            .whereEqualTo("partnerId", partnerId)
            .get()
    }

    /**
     * Retrieves a single order document by its unique ID.
     */
    fun getOrderById(orderId: String): Task<DocumentSnapshot> {
        return collection.document(orderId).get()
    }

    /**
     * Updates the status of an existing order.
     */
    fun updateOrderStatus(orderId: String, status: String): Task<Void> {
        return collection.document(orderId).update("status", status)
    }

    /**
     * Updates multiple fields of an order document.
     */
    fun updateOrder(orderId: String, updates: Map<String, Any>): Task<Void> {
        return collection.document(orderId).update(updates)
    }

    /**
     * Listens for real-time updates to a specific order.
     */
    fun listenToOrder(orderId: String, onUpdate: (com.sellora.partner.Project?) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return collection.document(orderId).addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null || !snapshot.exists()) {
                onUpdate(null)
                return@addSnapshotListener
            }
            onUpdate(mapToProject(snapshot))
        }
    }

    /**
     * Maps a Firestore DocumentSnapshot to a Project object.
     */
    fun mapToProject(doc: com.google.firebase.firestore.DocumentSnapshot): com.sellora.partner.Project {
        val data = doc.data ?: emptyMap()
        val createdAt = data["createdAt"] as? com.google.firebase.Timestamp
        val dateStr = if (createdAt != null) {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(createdAt.toDate())
        } else {
            "Unknown Date"
        }

        val serviceImageUrl = listOf(
            doc.getString("imageUrl"),
            doc.getString("image"),
            doc.getString("mediaUrl"),
            doc.getString("attachmentUrl"),
            doc.getString("serviceImage")
        ).firstOrNull { !it.isNullOrBlank() } ?: ""

        val clientPhotoUrl = listOf(
            doc.getString("clientPhotoUrl"),
            doc.getString("customerPhotoUrl"),
            doc.getString("clientPhoto"),
            doc.getString("customerPhoto"),
            doc.getString("customerPhotoUri"),
            doc.getString("photoUrl")
        ).firstOrNull { !it.isNullOrBlank() } ?: ""

        return com.sellora.partner.Project(
            id = doc.id,
            serviceName = data["serviceName"] as? String ?: "Untitled Service",
            freelancerName = data["freelancerName"] as? String ?: (data["clientName"] as? String ?: (data["customerName"] as? String ?: "Client")),
            date = dateStr,
            price = data["price"]?.toString() ?: "0",
            status = data["status"] as? String ?: "New",
            timer = "",
            serviceImageUrl = serviceImageUrl,
            clientPhotoUrl  = clientPhotoUrl,
            deliveryFileUrl = data["deliveryFileUrl"] as? String,
            requirementFileUrl = (data["requirementFileUrl"] ?: data["fileUrl"]) as? String,
            requirements = data["requirements"] as? String,
            deliveryDays = data["deliveryDays"]?.toString()?.toLongOrNull() ?: 0L,
            createdAt = createdAt?.toDate()?.time ?: 0L
        )
    }
}
