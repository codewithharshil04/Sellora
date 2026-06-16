package com.sellora.client

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object SupabaseUploader {

    private const val SUPABASE_URL = "https://aeeomqqoneptjqtiawyf.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFlZW9tcXFvbmVwdGpxdGlhd3lmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzM5OTkwNzAsImV4cCI6MjA4OTU3NTA3MH0.MCTSssWfDkGEIrxm_MaAqogfymJK7Q-ntmPpjWTxB34"
    private const val BUCKET      = "order-files"

    suspend fun uploadFile(context: Context, uri: Uri, orderId: String, isDelivery: Boolean = false): String? =
        withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                
                // 1. Get original filename from the URI
                var originalName = ""
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        originalName = cursor.getString(nameIndex)
                    }
                }

                // 2. Ensure we have a valid filename and extension
                if (originalName.isEmpty()) {
                    val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
                    originalName = "file_${System.currentTimeMillis()}.$ext"
                }

                // 3. Build a clean path: bucket/folder/orderId/filename
                val folder = if (isDelivery) "deliveries" else "attachments"
                val path = "$folder/$orderId/$originalName"
                val uploadUrl = "$SUPABASE_URL/storage/v1/object/$BUCKET/$path"

                val fileBytes = contentResolver.openInputStream(uri)
                    ?.use { it.readBytes() } ?: return@withContext null

                val conn = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput      = true
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Content-Type", mimeType)
                    setRequestProperty("x-upsert", "true")
                }

                DataOutputStream(conn.outputStream).use { it.write(fileBytes) }

                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_CREATED) {
                    "$SUPABASE_URL/storage/v1/object/public/$BUCKET/$path"
                } else {
                    val err = conn.errorStream?.bufferedReader()?.readText()
                    Log.e("Supabase", "Upload failed $code: $err")
                    null
                }
            } catch (e: Exception) {
                Log.e("Supabase", "Upload exception", e)
                null
            }
        }

    suspend fun downloadFile(context: Context, fileUrl: String, orderId: String): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(fileUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connect()

                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e("Supabase", "Download failed: ${conn.responseCode}")
                    return@withContext null
                }

                // Get file extension from URL more safely
                val cleanUrl = fileUrl.substringBefore('?')
                val extension = cleanUrl.substringAfterLast(".", "tmp")
                val fileName = "order_${orderId}_file.$extension"

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
                Log.e("Supabase", "Download exception", e)
                null
            }
        }

    suspend fun deleteFile(fileUrl: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                // Extract file path from URL
                val filePath = fileUrl.substringAfterLast("/object/public/$BUCKET/")
                val deleteUrl = "$SUPABASE_URL/storage/v1/object/$BUCKET/$filePath"

                val conn = URL(deleteUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")

                val responseCode = conn.responseCode
                responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT
            } catch (e: Exception) {
                Log.e("Supabase", "Delete exception", e)
                false
            }
        }
}