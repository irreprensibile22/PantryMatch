package com.example.esamemobile.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Una ricetta pubblicata da un utente.
 *
 * [ingredientNames] è la versione denormalizzata (nomi normalizzati) usata per
 * le query Firestore `whereArrayContainsAny`, così da pre-filtrare le ricette
 * candidate prima del match fine lato client (vedi IngredientMatcher).
 */
data class Recipe(
    @DocumentId
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val ingredientNames: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val prepTimeMin: Int = 0,
    val difficulty: String = Difficulty.EASY.name,
    val category: String = "",
    // Senza @PropertyName Firestore serializzerebbe il getter isPublic() come
    // campo "public" (convenzione JavaBean sui boolean is-): così la query
    // whereEqualTo("isPublic", true) non troverebbe nulla. Forziamo il nome.
    @get:PropertyName("isPublic") @set:PropertyName("isPublic")
    var isPublic: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class Difficulty { EASY, MEDIUM, HARD }
