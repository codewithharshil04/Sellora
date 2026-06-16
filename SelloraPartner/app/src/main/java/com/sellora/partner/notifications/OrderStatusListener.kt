package com.sellora.partner.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class OrderStatusListener(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationHelper = NotificationHelper(context)
    private var isListening = false
    private var initialLoadDone = false
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    fun startListening(uid: String) {
        if (isListening) return
        
        initialLoadDone = false
        // Listen for real-time updates on partner's orders
        listenerRegistration = db.collection("orders")
            .whereEqualTo("partnerId", uid)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("PartnerOrderListener", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (!initialLoadDone) {
                    initialLoadDone = true
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { docChange ->
                    val orderId = docChange.document.id
                    val order = docChange.document.data
                    when (docChange.type) {
                        DocumentChange.Type.ADDED -> {
                            // New order received
                            handleNewOrder(orderId, order)
                        }
                        DocumentChange.Type.MODIFIED -> {
                            // Order status updated
                            handleOrderStatusChange(orderId, order)
                        }
                        DocumentChange.Type.REMOVED -> {
                            // Order cancelled
                            handleOrderCancellation(orderId, order)
                        }
                    }
                }
            }
        
        isListening = true
        Log.d("PartnerOrderListener", "Started listening for order updates for partner: $uid")
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
        isListening = false
        Log.d("PartnerOrderListener", "Stopped listening for order updates")
    }

    private fun handleNewOrder(orderId: String, order: Map<String, Any?>) {
        val serviceName = order["serviceName"] as? String ?: "Service"
        val clientName = order["clientName"] as? String ?: "Client"
        val price = order["price"] as? String ?: "₹0"
        val status = order["status"] as? String ?: "New"

        // Only show notification for new orders (not when loading existing ones)
        if (status == "New") {
            notificationHelper.showNewOrderNotification(
                orderId = orderId,
                serviceName = serviceName,
                clientName = clientName,
                price = price
            )
        }

        Log.d("PartnerOrderListener", "New order notification sent for order $orderId")
    }

    private fun handleOrderStatusChange(orderId: String, order: Map<String, Any?>) {
        val serviceName = order["serviceName"] as? String ?: "Service"
        val clientName = order["clientName"] as? String ?: "Client"
        val status = order["status"] as? String ?: return

        // Don't send notification for "New" status updates (only for new orders)
        if (status != "New") {
            notificationHelper.showOrderStatusNotification(
                orderId = orderId,
                serviceName = serviceName,
                clientName = clientName,
                status = status
            )
        }

        Log.d("PartnerOrderListener", "Status notification sent for order $orderId with status: $status")
    }

    private fun handleOrderCancellation(orderId: String, order: Map<String, Any?>) {
        val serviceName = order["serviceName"] as? String ?: "Service"
        val clientName = order["clientName"] as? String ?: "Client"

        notificationHelper.showOrderStatusNotification(
            orderId = orderId,
            serviceName = serviceName,
            clientName = clientName,
            status = "Cancelled"
        )

        Log.d("PartnerOrderListener", "Cancellation notification sent for order $orderId")
    }
}
