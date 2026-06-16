package com.sellora.partner.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import com.sellora.partner.BuildConfig
import com.sellora.partner.api.CloudinaryApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSink
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * MediaRepository — single source of truth for all media uploads in the Partner app.
 */
object MediaRepository {

    private val CLOUD_NAME    = BuildConfig.CLOUDINARY_CLOUD_NAME
    private val UPLOAD_PRESET = BuildConfig.CLOUDINARY_UPLOAD_PRESET
    private const val TAG           = "MediaRepository"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.cloudinary.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(CloudinaryApi::class.java)

    /**
     * Uploads any image or video URI to Cloudinary and returns the secure HTTPS URL.
     */
    suspend fun upload(context: Context, uri: Uri, folder: String? = null): String? {
        return uploadInternal(context, uri, folder).first
    }

    private suspend fun uploadInternal(context: Context, uri: Uri, folder: String? = null): Pair<String?, String?> = withContext(Dispatchers.IO) {
        if (CLOUD_NAME == "null" || CLOUD_NAME.isEmpty()) {
            val err = "Cloudinary Cloud Name is not configured in local.properties"
            Log.e(TAG, err)
            return@withContext Pair(null, err)
        }
        try {
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val isVideo  = mimeType.startsWith("video/")
            val resourceType = if (isVideo) "video" else "auto"

            val requestFile = object : RequestBody() {
                override fun contentType() = mimeType.toMediaTypeOrNull()

                override fun contentLength(): Long {
                    return try {
                        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                    } catch (e: Exception) {
                        -1L
                    }
                }

                override fun writeTo(sink: BufferedSink) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw java.io.IOException("Could not open input stream for $uri")
                    
                    inputStream.use { input ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            sink.write(buffer, 0, read)
                        }
                    }
                }
            }
            val filePart = MultipartBody.Part.createFormData("file", getFileName(context, uri), requestFile)
            val presetPart = UPLOAD_PRESET.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = api.uploadFile(CLOUD_NAME, resourceType, presetPart, filePart)

            if (response.isSuccessful) {
                val body = response.body()
                val url = body?.get("secure_url") as? String
                if (!url.isNullOrEmpty()) {
                    return@withContext Pair(url, null)
                } else {
                    val err = "Cloudinary returned empty secure_url"
                    Log.e(TAG, "$err. Response: $body")
                    return@withContext Pair(null, err)
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "no error body"
                val err = "Upload failed HTTP ${response.code()}: $errorBody"
                Log.e(TAG, err)
                return@withContext Pair(null, err)
            }
        } catch (e: java.net.UnknownHostException) {
            val err = "Network error: Unable to resolve host. Check your internet connection."
            Log.e(TAG, err, e)
            return@withContext Pair(null, err)
        } catch (e: java.net.SocketTimeoutException) {
            val err = "Upload timed out. The file might be too large or your connection is slow."
            Log.e(TAG, err, e)
            return@withContext Pair(null, err)
        } catch (e: Exception) {
            val err = "Upload exception: ${e.localizedMessage}"
            Log.e(TAG, err, e)
            return@withContext Pair(null, err)
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = it.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "upload_file"
    }

    /**
     * Convenience wrapper that returns a pair of (URL, error message) for the upload.
     */
    suspend fun uploadWithError(context: Context, uri: Uri): Pair<String?, String?> {
        return uploadInternal(context, uri)
    }
}
