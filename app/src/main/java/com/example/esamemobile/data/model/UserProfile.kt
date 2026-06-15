package com.example.esamemobile.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Profilo pubblico di un utente.
 */
data class UserProfile(
    @DocumentId
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
)
