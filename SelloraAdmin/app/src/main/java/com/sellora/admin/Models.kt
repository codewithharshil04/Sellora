package com.sellora.admin

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AdminUser(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "client"
) : Parcelable

@Parcelize
data class AdminPartner(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val isActive: Boolean = true,
    val totalServices: Int = 0
) : Parcelable

@Parcelize
data class AdminService(
    val id: String = "",
    val serviceName: String = "",
    val partnerName: String = "",
    val freelancerName: String = "",
    val category: String = "",
    val description: String = "",
    val minPrice: String = "",
    val maxPrice: String = "",
    val basicPrice: String = "",
    val advPrice: String = "",
    val proPrice: String = "",
    val deliveryTime: String = "",
    val isActive: Boolean = true,
    val imageUrl: String = "",
    val freelancerPhotoUrl: String = "",
    val deliverables: List<String> = emptyList(),
    val deliverableTiers: List<String> = emptyList()
) : Parcelable

@Parcelize
data class AdminOrder(
    val id: String = "",
    val serviceName: String = "",
    val clientName: String = "",
    val partnerName: String = "",
    val price: String = "",
    val status: String = "New",
    val date: String = "",
    val packageType: String = "",
    val requirements: String = "",
    val fileUrl: String = ""
) : Parcelable
