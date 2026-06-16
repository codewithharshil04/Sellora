package com.sellora.partner

import android.net.Uri
import android.util.Base64
import android.widget.ImageView
import coil.load

/**
 * Robust image loader for profile photos.
 * Handles: Uri objects (local selection), Base64 strings (Firestore), HTTP URLs, and local paths.
 */
fun ImageView.loadProfileImage(data: Any?, placeholderRes: Int) {
    if (data == null || (data is String && data.isBlank())) {
        load(placeholderRes)
        return
    }

    // Identify the best way to load the data using Coil
    val requestData: Any = when (data) {
        is Uri -> data
        is ByteArray -> data
        is String -> {
            // Check if it's a URI scheme or a file path
            if (data.startsWith("http") || data.startsWith("content://") || 
                data.startsWith("file://") || data.startsWith("/")) {
                data
            } else {
                // Attempt to decode as Base64 (typical for Firestore photoUrl in this app)
                try {
                    val clean = if (data.contains(",")) data.substringAfter(",") else data
                    Base64.decode(clean, Base64.DEFAULT)
                } catch (_: Exception) {
                    data // Fallback to raw string if decode fails
                }
            }
        }
        else -> data
    }

    load(requestData) {
        placeholder(placeholderRes)
        error(placeholderRes)
        crossfade(true)
    }
}
