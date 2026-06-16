package com.sellora.partner.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.sellora.partner.BuildConfig
import com.sellora.partner.api.SupabaseApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

object SupabaseRepository {
    private const val TAG = "SupabaseRepository"
    private const val BUCKET_NAME = "order-files"
    private const val FOLDER_DELIVERIES = "deliveries"
    private const val FOLDER_ATTACHMENTS = "attachments"
    private val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private val SUPABASE_KEY = BuildConfig.SUPABASE_KEY

    private val client = OkHttpClient.Builder().build()
    private val retrofit = Retrofit.Builder()
        .baseUrl(if (SUPABASE_URL.endsWith("/")) SUPABASE_URL else "$SUPABASE_URL/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(SupabaseApi::class.java)

    suspend fun uploadFile(
        context: Context,
        uri: Uri,
        orderId: String,
        folder: String = FOLDER_DELIVERIES,
        bucket: String = BUCKET_NAME
    ): Pair<String?, String?> = withContext(Dispatchers.IO) {
        try {
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val originalName = getFileName(context, uri)
            val fileName = "$folder/$orderId/$originalName"

            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext Pair(null, "Could not open file")
            val bytes = inputStream.readBytes()
            val requestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())

            val response = api.uploadFile(
                bucket = bucket,
                path = fileName,
                contentType = mime,
                apiKey = SUPABASE_KEY,
                authHeader = "Bearer $SUPABASE_KEY",
                file = requestBody
            )

            if (response.isSuccessful) {
                // Construct the public URL
                val publicUrl = "${SUPABASE_URL}/storage/v1/object/public/$bucket/$fileName"
                Pair(publicUrl, null)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Upload failed: $errorMsg")
                Pair(null, errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload exception", e)
            Pair(null, e.localizedMessage)
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "file"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}
