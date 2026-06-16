package com.sellora.partner.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface CloudinaryApi {
    @Multipart
    @POST("v1_1/{cloud_name}/{resource_type}/upload")
    suspend fun uploadFile(
        @Path("cloud_name") cloudName: String,
        @Path("resource_type") resourceType: String,
        @Part("upload_preset") uploadPreset: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<Map<String, Any>>
}
