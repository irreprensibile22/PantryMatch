package com.example.pantrymatch.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

// Recipe stored in Firestore. Default values are required by Firestore for deserialization.
data class Recipe(
    @DocumentId
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val ingredients: List<RecipeIngredient> = emptyList(),
    // normalized names duplicated here so they can be used in Firestore array-contains queries
    val ingredientNames: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val prepTimeMin: Int = 0,
    val difficulty: String = Difficulty.EASY.name,
    val category: String = "",
    // without PropertyName Firestore would serialize the "is..." getter as a "public" field: force it to "isPublic"
    @get:PropertyName("isPublic") @set:PropertyName("isPublic")
    var isPublic: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class Difficulty { EASY, MEDIUM, HARD }
