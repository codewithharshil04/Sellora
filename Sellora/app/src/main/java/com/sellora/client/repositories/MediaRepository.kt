package com.sellora.client.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * MediaRepository — single source of truth for all media uploads in the Client app.
 *
 * Supports:
 *  - Images  (image/jpeg, image/png, image/webp, …)
 *  - Videos  (video/mp4, video/3gpp, …)
 *
 * Usage (from a coroutine / lifecycleScope):
 *   val url = MediaRepository.upload(context, uri)
 *   if (url != null) { /* use url */ } else { /* handle failure */ }
 */
object MediaRepository {

    private const val CLOUD_NAME    = "dertbslxi"
    private const val UPLOAD_PRESET = "sellora_upload"
    private const val TAG           = "MediaRepository"

    /**
     * Uploads any image or video URI to Cloudinary.
     * Returns the secure HTTPS URL on success, or null on failure.
     * Must be called from a coroutine — performs IO on Dispatchers.IO.
     */
    suspend fun upload(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val isVideo  = mimeType.startsWith("video/")
            val resource = if (isVideo) "video" else "image"
            val endpoint = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/$resource/upload"

            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: run { Log.e(TAG, "Cannot open input stream for $uri"); return@withContext null }

            val boundary = "----SellBoundary${System.currentTimeMillis()}"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod  = "POST"
                doOutput       = true
                connectTimeout = 30_000
                readTimeout    = 60_000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            DataOutputStream(conn.outputStream).use { dos ->
                dos.writeBytes("--$boundary\r\n")
                dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
                dos.writeBytes("$UPLOAD_PRESET\r\n")

                val filename = if (isVideo) "profile_video" else "profile_image"
                dos.writeBytes("--$boundary\r\n")
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$filename\"\r\n")
                dos.writeBytes("Content-Type: $mimeType\r\n\r\n")
                dos.write(bytes)
                dos.writeBytes("\r\n--$boundary--\r\n")
                dos.flush()
            }

            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val url  = json.optString("secure_url", "")
                if (url.isNotEmpty()) url else {
                    Log.e(TAG, "Cloudinary returned empty URL")
                    null
                }
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "no body"
                Log.e(TAG, "Upload failed HTTP $code: $err")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload exception", e)
            null
        }
    }
}
