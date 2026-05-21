package com.example.vowera

data class Vendor(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val email: String = "",
    val cost: String = "",
    val status: String = "pending", // pending, confirmed, completed
    val timestamp: Long = System.currentTimeMillis()
)

