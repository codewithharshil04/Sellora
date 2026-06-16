package com.sellora.partner.api

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface SupabaseApi {
    /**
     * Uploads a file to Supabase Storage.
     * Path should include the filename.
     */
    @POST("storage/v1/object/{bucket}/{path}")
    suspend fun uploadFile(
        @Path("bucket") bucket: String,
        @Path("path") path: String,
        @Header("Content-Type") contentType: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body file: RequestBody
    ): Response<Map<String, Any>>
}
