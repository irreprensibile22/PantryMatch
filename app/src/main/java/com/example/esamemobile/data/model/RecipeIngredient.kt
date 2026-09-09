package com.example.esamemobile.data.model

// Singolo ingrediente di una ricetta. optional = non conta tra i "mancanti" nel matching.
data class RecipeIngredient(
    val name: String = "",
    val quantity: Double? = null,
    val unit: String? = null,
    val optional: Boolean = false,
)
