package com.example.esamemobile.data.model

import com.google.firebase.firestore.DocumentId

// Profilo pubblico dell'utente (collezione users/{uid}).
data class UserProfile(
    @DocumentId
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
)
