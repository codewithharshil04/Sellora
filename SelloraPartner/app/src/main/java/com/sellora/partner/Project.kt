package com.sellora.partner

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Project(
    val id: String = "",
    val serviceName: String = "",
    val freelancerName: String = "",  // = Client name on freelancer side
    val date: String = "",
    val price: String = "0",
    val status: String = "",
    val timer: String = "",
    val serviceImageUrl: String = "",
    val clientPhotoUrl: String = "",
    val deliveryFileUrl: String? = null,
    val requirementFileUrl: String? = null,
    val requirements: String? = null,
    val deliveryDays: Long = 0,
    val createdAt: Long = 0
) {
    /**
     * Returns the numeric price value, cleaning currency symbols.
     */
    fun getNumericPrice(): Double {
        return price.replace("₹", "").replace(",", "").toDoubleOrNull() ?: 0.0
    }

    /**
     * Returns the net earning after platform fee deduction.
     */
    fun getNetEarning(): Double {
        return getNumericPrice() * Constants.PLATFORM_FEE_RATE
    }
}
