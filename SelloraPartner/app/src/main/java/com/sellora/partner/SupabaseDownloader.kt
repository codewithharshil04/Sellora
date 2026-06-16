package com.sellora.partner

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object SupabaseDownloader {

    suspend fun downloadFile(context: Context, fileUrl: String, orderId: String): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(fileUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connect()

                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e("SupabaseDownloader", "Download failed: ${conn.responseCode}")
                    return@withContext null
                }

                // Get content type from response headers
                val contentType = conn.contentType ?: "application/octet-stream"
                val mimeType = contentType.split(";")[0].trim()
                
                // Extract file extension from MIME type or URL
                val extension = when {
                    mimeType.startsWith("image/jpeg") -> "jpg"
                    mimeType.startsWith("image/png") -> "png"
                    mimeType.startsWith("image/gif") -> "gif"
                    mimeType.startsWith("image/webp") -> "webp"
                    mimeType.startsWith("application/pdf") -> "pdf"
                    mimeType.startsWith("text/") -> "txt"
                    mimeType.startsWith("application/zip") -> "zip"
                    mimeType.startsWith("video/mp4") -> "mp4"
                    mimeType.startsWith("video/") -> {
                        // Extract from URL for video files
                        fileUrl.substringAfterLast(".", "").substringBefore("?").ifEmpty { "mp4" }
                    }
                    else -> {
                        // Fallback to URL extraction
                        fileUrl.substringAfterLast(".", "").substringBefore("?").ifEmpty { "tmp" }
                    }
                }
                
                val fileName = "order_${orderId}_${System.currentTimeMillis()}.$extension"

                // Create temp file in app's cache directory
                val tempFile = File(context.cacheDir, fileName)

                // Download file
                FileOutputStream(tempFile).use { output ->
                    conn.inputStream.use { input ->
                        input.copyTo(output)
                    }
                }

                // Return content URI using FileProvider
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    tempFile
                )
            } catch (e: Exception) {
                Log.e("SupabaseDownloader", "Download exception", e)
                null
            }
        }
}
