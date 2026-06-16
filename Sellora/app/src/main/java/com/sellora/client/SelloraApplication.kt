package com.sellora.client

import android.app.Application
import com.sellora.client.notifications.OrderStatusListener

class SelloraApplication : Application() {

    private lateinit var orderStatusListener: OrderStatusListener

    override fun onCreate() {
        super.onCreate()
        
        // Clear image cache on start to ensure fresh profile photos
        coil.Coil.imageLoader(this).memoryCache?.clear()
    }

    fun startOrderListener(uid: String) {
        orderStatusListener = OrderStatusListener(this)
        orderStatusListener.startListening(uid)
    }
}
