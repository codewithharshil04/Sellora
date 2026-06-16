package com.sellora.client

data class Service(
    val id                 : String = "",
    val imageUrl           : String = "",
    val freelancerName     : String = "",
    val freelancerPhotoUrl : String = "",
    val priceRange         : String = "",
    val serviceName        : String = "",
    val partnerId          : String = "",
    val category           : String = "Other",
    val description        : String = "No description provided.",
    val basicPrice         : String = "₹999",
    val advPrice           : String = "₹1999",
    val proPrice           : String = "₹2999",
    val deliverables       : List<String> = emptyList(),
    val deliverableTiers   : List<String> = emptyList(),
    val deliveryTime       : String = ""   // e.g. "3" (days)
)
