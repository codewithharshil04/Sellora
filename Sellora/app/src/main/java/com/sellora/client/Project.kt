package com.sellora.client

data class Project(
    val id: String = "",
    val serviceName: String,
    val freelancerName: String,
    val date: String,
    val price: String,
    val status: String,
    val timer: String = "00 : 00 : 00",
    val serviceImageUrl: String = "",
    val freelancerPhotoUrl: String = "",
    val deliveryFileUrl: String? = null,
    val requirementFileUrl: String? = null
)