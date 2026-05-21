package com.example.vowera

data class PremiumPackage(
    val id: String,
    val name: String,
    val price: Double,
    val description: String,
    val features: List<String>
)

