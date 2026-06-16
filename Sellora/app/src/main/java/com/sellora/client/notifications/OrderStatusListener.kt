package com.sellora.client.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore

class OrderStatusListener(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val notificationHelper = NotificationHelper(context)
    private var isListening = false
    private var initialLoadDone = false

    fun startListening(uid: String) {
        if (isListening) return
        
        initialLoadDone = false
        // Listen for real-time updates on user's orders
        db.collection("orders")
            .whereEqualTo("clientId", uid)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("OrderStatusListener", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshots != null && !initialLoadDone) {
                    initialLoadDone = true
                    Log.d("OrderStatusListener", "Initial load complete, skipping notifications for existing orders")
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { docChange ->
                    val orderId = docChange.document.id
                    when (docChange.type) {
                        DocumentChange.Type.ADDED -> {
                            // New order placed
                            val order = docChange.document.data
                            handleNewOrder(orderId, order)
                        }
                        DocumentChange.Type.MODIFIED -> {
                            // Order status updated
                            val order = docChange.document.data
                            handleOrderStatusChange(orderId, order)
                        }
                        DocumentChange.Type.REMOVED -> {
                            // Order cancelled
                            val order = docChange.document.data
                            handleOrderCancellation(orderId, order)
                        }
                    }
                }
            }
        
        isListening = true
        Log.d("OrderStatusListener", "Started listening for order updates for user: $uid")
    }

    fun stopListening() {
        isListening = false
        Log.d("OrderStatusListener", "Stopped listening for order updates")
    }

    private fun handleNewOrder(orderId: String, order: Map<String, Any?>) {
        val serviceName = order["serviceName"] as? String ?: "Service"
        val partnerName = order["partnerName"] as? String ?: "Freelancer"
        val status = order["status"] as? String ?: "New"

        // Only show notification for new orders (not when loading existing ones)
        if (status == "New") {
            notificationHelper.showOrderNotification(
                notificationType = NotificationHelper.NOTIFICATION_ORDER_NEW,
                orderId = orderId,
                serviceName = serviceName,
                freelancerName = partnerName,
                status = status
            )
        }

        Log.d("OrderStatusListener", "New order notification sent for order $orderId")
    }

    private fun handleOrderStatusChange(orderId: String, order: Map<String, Any?>) {
        val status = order["status"] as? String ?: return
        if (status != "New") {
            val serviceName = order["serviceName"] as? String ?: "Service"
            val freelancerName = order["freelancerName"] as? String ?: "Freelancer"
            
            val notificationType = when (status) {
                "In Progress" -> NotificationHelper.NOTIFICATION_ORDER_IN_PROGRESS
                "Delivered" -> NotificationHelper.NOTIFICATION_ORDER_DELIVERED
                "Cancelled" -> NotificationHelper.NOTIFICATION_ORDER_CANCELLED
                else -> return
            }

            notificationHelper.showOrderNotification(
                notificationType = notificationType,
                orderId = orderId,
                serviceName = serviceName,
                freelancerName = freelancerName,
                status = status
            )

            Log.d("OrderStatusListener", "Status notification sent for order $orderId with status: $status")
        }
    }

    private fun handleOrderCancellation(orderId: String, order: Map<String, Any?>) {
        val serviceName = order["serviceName"] as? String ?: "Service"
        val freelancerName = order["freelancerName"] as? String ?: "Freelancer"

        notificationHelper.showOrderNotification(
            notificationType = NotificationHelper.NOTIFICATION_ORDER_CANCELLED,
            orderId = orderId,
            serviceName = serviceName,
            freelancerName = freelancerName,
            status = "Cancelled"
        )

        Log.d("OrderStatusListener", "Cancellation notification sent for order $orderId")
    }
}
