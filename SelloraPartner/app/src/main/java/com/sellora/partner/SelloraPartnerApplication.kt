package com.sellora.partner

import android.app.Application
import android.util.Log
import com.google.android.gms.security.ProviderInstaller
import com.sellora.partner.notifications.OrderStatusListener

class SelloraPartnerApplication : Application() {

    private var orderStatusListener: OrderStatusListener? = null

    override fun onCreate() {
        super.onCreate()

        // Install security provider to fix SSL issues on older devices / GMS versions
        try {
            ProviderInstaller.installIfNeededAsync(this, object : ProviderInstaller.ProviderInstallListener {
                override fun onProviderInstalled() {
                    Log.d("SelloraApp", "Security provider installed successfully")
                }
                override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: android.content.Intent?) {
                    Log.e("SelloraApp", "Security provider installation failed: $errorCode")
                }
            })
        } catch (e: Exception) {
            Log.e("SelloraApp", "Security provider installation exception", e)
        }

        // Clear Coil cache on app start
        coil.Coil.imageLoader(this).memoryCache?.clear()
    }

    private var currentListeningUid: String? = null

    fun startOrderListener(uid: String) {
        if (orderStatusListener == null || currentListeningUid != uid) {
            orderStatusListener?.stopListening()
            orderStatusListener = OrderStatusListener(this)
            orderStatusListener?.startListening(uid)
            currentListeningUid = uid
        }
    }

    fun stopOrderListener() {
        orderStatusListener?.stopListening()
        orderStatusListener = null
        currentListeningUid = null
    }
}
