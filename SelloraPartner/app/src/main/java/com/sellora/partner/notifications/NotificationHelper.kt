package com.sellora.partner.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sellora.partner.R
import com.sellora.partner.DashboardActivity
import com.sellora.partner.ProjectsActivity

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "sellora_partner_orders"
        private const val CHANNEL_NAME = "Order Updates"
        private const val CHANNEL_DESCRIPTION = "Notifications for new orders and status updates"
        
        const val NOTIFICATION_NEW_ORDER = 2001
        const val NOTIFICATION_ORDER_UPDATE = 2002
        const val NOTIFICATION_PAYMENT_RECEIVED = 2003
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

    fun showNewOrderNotification(
        orderId: String,
        serviceName: String,
        clientName: String,
        price: String
    ) {
        val title = "New Order Received! 🎉"
        val message = "$clientName has placed an order for $serviceName worth $price"
        
        val intent = Intent(context, ProjectsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("highlight_order_id", orderId)
        }
        
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
            notify(NOTIFICATION_NEW_ORDER, notification)
        }
    }

    fun showOrderStatusNotification(
        orderId: String,
        serviceName: String,
        clientName: String,
        status: String
    ) {
        val (title, message) = getNotificationContent(status, serviceName, clientName)
        val intent = createNotificationIntent(orderId)
        
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
            notify(NOTIFICATION_ORDER_UPDATE, notification)
        }
    }

    fun showPaymentNotification(
        orderId: String,
        serviceName: String,
        amount: String
    ) {
        val title = "Payment Received! 💰"
        val message = "Payment of $amount received for $serviceName order"
        
        val intent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
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
            notify(NOTIFICATION_PAYMENT_RECEIVED, notification)
        }
    }

    private fun getNotificationContent(
        status: String,
        serviceName: String,
        clientName: String
    ): Pair<String, String> {
        return when (status) {
            "In Progress" -> Pair(
                "Order In Progress",
                "You started working on $serviceName for $clientName"
            )
            "Delivered" -> Pair(
                "Order Delivered! 🎉",
                "You have successfully delivered $serviceName to $clientName"
            )
            "Cancelled" -> Pair(
                "Order Cancelled",
                "Order for $serviceName has been cancelled by $clientName"
            )
            else -> Pair(
                "Order Update",
                "$serviceName order status updated to $status"
            )
        }
    }

    private fun createNotificationIntent(orderId: String): Intent {
        return Intent(context, ProjectsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("highlight_order_id", orderId)
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
