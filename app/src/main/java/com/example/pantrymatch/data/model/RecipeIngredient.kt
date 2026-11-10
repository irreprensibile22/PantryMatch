package com.example.pantrymatch.data.model

// A single recipe ingredient. optional = not counted as "missing" during matching.
data class RecipeIngredient(
    val name: String = "",
    val quantity: Double? = null,
    val unit: String? = null,
    val optional: Boolean = false,
)
