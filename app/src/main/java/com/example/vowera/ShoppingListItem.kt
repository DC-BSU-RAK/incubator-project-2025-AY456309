package com.example.vowera

data class ShoppingListItem(
    val id: String = "",
    val name: String = "",
    val quantity: String = "",
    val category: String = "",
    val isCompleted: Boolean = false
)
