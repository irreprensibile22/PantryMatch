package com.example.esamemobile.data.model

import com.google.firebase.firestore.DocumentId

// Voce della dispensa dell'utente (sotto-collezione users/{uid}/pantry).
data class PantryItem(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val quantity: Double? = null,
    val unit: String? = null,
)
