package com.sellora.partner

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class FreelancerService(
    val id              : String = "",
    val partnerId       : String = "",
    val partnerName     : String = "",
    val freelancerName  : String = "",
    val name            : String = "",
    val description     : String = "",
    val category        : String = "",
    val minPrice        : String = "",
    val maxPrice        : String = "",
    val basicPrice      : String = "",
    val advPrice        : String = "",
    val proPrice        : String = "",
    val deliveryTime    : String = "",
    val imageUri        : String = "",
    val deliverables    : List<String> = emptyList(),
    val deliverableTiers: List<String> = emptyList(), // "Basic" | "Adv" | "Pro" per deliverable
    val isActive        : Boolean = true
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.createStringArrayList() ?: emptyList(),
        parcel.createStringArrayList() ?: emptyList(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(partnerId)
        parcel.writeString(partnerName)
        parcel.writeString(freelancerName)
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeString(category)
        parcel.writeString(minPrice)
        parcel.writeString(maxPrice)
        parcel.writeString(basicPrice)
        parcel.writeString(advPrice)
        parcel.writeString(proPrice)
        parcel.writeString(deliveryTime)
        parcel.writeString(imageUri)
        parcel.writeStringList(deliverables)
        parcel.writeStringList(deliverableTiers)
        parcel.writeByte(if (isActive) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<FreelancerService> {
        override fun createFromParcel(parcel: Parcel): FreelancerService = FreelancerService(parcel)
        override fun newArray(size: Int): Array<FreelancerService?> = arrayOfNulls(size)
    }
}
