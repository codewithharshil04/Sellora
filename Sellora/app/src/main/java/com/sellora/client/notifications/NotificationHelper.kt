package com.sellora.client.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sellora.client.R
import com.sellora.client.HomeActivity
import com.sellora.client.ProjectsActivity

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "sellora_orders"
        private const val CHANNEL_NAME = "Order Updates"
        private const val CHANNEL_DESCRIPTION = "Notifications for order status updates"
        
        const val NOTIFICATION_ORDER_NEW = 1001
        const val NOTIFICATION_ORDER_IN_PROGRESS = 1002
        const val NOTIFICATION_ORDER_DELIVERED = 1003
        const val NOTIFICATION_ORDER_CANCELLED = 1004
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showOrderNotification(
        notificationType: Int,
        orderId: String,
        serviceName: String,
        freelancerName: String,
        status: String
    ) {
        val (title, message) = getNotificationContent(status, serviceName, freelancerName)
        val intent = createNotificationIntent(notificationType, orderId)
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            orderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(notificationType, notification)
        }
    }

    private fun getNotificationContent(
        status: String,
        serviceName: String,
        freelancerName: String
    ): Pair<String, String> {
        return when (status) {
            "New" -> Pair(
                "Order Placed Successfully!",
                "Your order for $serviceName has been placed with $freelancerName and is now being processed."
            )
            "In Progress" -> Pair(
                "Order In Progress",
                "$freelancerName has started working on your $serviceName order."
            )
            "Delivered" -> Pair(
                "Order Delivered! 🎉",
                "Great news! Your $serviceName order has been completed by $freelancerName. Check your projects to review."
            )
            "Cancelled" -> Pair(
                "Order Cancelled",
                "Your order for $serviceName has been cancelled. Please contact support for more details."
            )
            else -> Pair(
                "Order Update",
                "Your $serviceName order status has been updated to $status"
            )
        }
    }

    private fun createNotificationIntent(notificationType: Int, orderId: String): Intent {
        return when (notificationType) {
            NOTIFICATION_ORDER_NEW -> Intent(context, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            else -> Intent(context, ProjectsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("highlight_order_id", orderId)
            }
        }
    }

    fun clearNotification(notificationId: Int) {
        with(NotificationManagerCompat.from(context)) {
            cancel(notificationId)
        }
    }

    fun clearAllNotifications() {
        with(NotificationManagerCompat.from(context)) {
            cancelAll()
        }
    }
}
