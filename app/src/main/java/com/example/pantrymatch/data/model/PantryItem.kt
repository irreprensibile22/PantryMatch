package com.example.pantrymatch.data.model

import com.google.firebase.firestore.DocumentId

// An item in the user's pantry (sub-collection users/{uid}/pantry).
data class PantryItem(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val quantity: Double? = null,
    val unit: String? = null,
)
