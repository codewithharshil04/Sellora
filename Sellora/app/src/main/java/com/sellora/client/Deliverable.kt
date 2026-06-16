package com.sellora.client

data class Deliverable(
    val name: String,
    val tier: String = "Basic"   // "Basic" | "Adv" | "Pro"
)

fun Deliverable.isIncludedIn(plan: String): Boolean = when (plan) {
    "Basic" -> tier == "Basic"
    "Adv"   -> tier == "Basic" || tier == "Adv"
    "Pro"   -> true
    else    -> tier == "Basic"
}