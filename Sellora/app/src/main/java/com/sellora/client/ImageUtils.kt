package com.sellora.client

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import coil.load
import coil.request.CachePolicy

/**
 * Enhanced image loading helper for Sellora Client.
 * Optimized for RecyclerView performance.
 */
fun ImageView.loadImage(data: Any?, placeholderRes: Int) {
    val source = data as? String
    val cleanUrl = source?.trim()

    if (cleanUrl.isNullOrEmpty() || cleanUrl == "null") {
        setImageResource(placeholderRes)
        return
    }

    // 1. If it starts with http, content:// or file://, it is a valid URL/URI.
    if (cleanUrl.startsWith("http") || cleanUrl.startsWith("content://") || cleanUrl.startsWith("file://")) {
        load(cleanUrl) {
            placeholder(placeholderRes)
            error(placeholderRes)
            crossfade(150) // Reduced duration for better feel
            memoryCachePolicy(CachePolicy.ENABLED)
            diskCachePolicy(CachePolicy.ENABLED)
            // Optimization: allow hardware bitmaps for performance
            allowHardware(true)
        }
        return
    }

    // 2. Try to decode as Base64 if it looks like one.
    try {
        val clean = if (cleanUrl.contains(",")) cleanUrl.substringAfter(",") else cleanUrl
        val base64Clean = clean.replace("\n", "").replace("\r", "").replace(" ", "")
        
        if (base64Clean.length > 32) {
            val bytes = Base64.decode(base64Clean, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bmp != null) {
                setImageBitmap(bmp)
                return 
            }
        }
    } catch (e: Exception) {
        Log.e("ImageUtils", "Base64 decode error: ${e.message}")
    }

    // Final Fallback
    setImageResource(placeholderRes)
}

fun ImageView.loadProfileImage(data: Any?, placeholderRes: Int) = loadImage(data, placeholderRes)
