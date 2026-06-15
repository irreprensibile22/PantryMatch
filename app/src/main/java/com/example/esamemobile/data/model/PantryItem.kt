package com.example.esamemobile.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Un ingrediente presente nell'inventario (dispensa) dell'utente.
 */
data class PantryItem(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val quantity: Double? = null,
    val unit: String? = null,
)
