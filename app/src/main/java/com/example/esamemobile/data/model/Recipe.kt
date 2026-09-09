package com.example.esamemobile.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

// Ricetta salvata su Firestore. I valori di default servono a Firestore per deserializzare.
data class Recipe(
    @DocumentId
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val ingredients: List<RecipeIngredient> = emptyList(),
    // nomi normalizzati duplicati qui per poterli usare nelle query array-contains di Firestore
    val ingredientNames: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val prepTimeMin: Int = 0,
    val difficulty: String = Difficulty.EASY.name,
    val category: String = "",
    // senza PropertyName Firestore serializzerebbe il getter "is..." come campo "public": lo forzo a "isPublic"
    @get:PropertyName("isPublic") @set:PropertyName("isPublic")
    var isPublic: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class Difficulty { EASY, MEDIUM, HARD }
