package com.example.vowera

data class Guest(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val side: String = "",
    val rsvp: String = "Pending",
    val events: List<String> = emptyList()
)