package com.example.pantrymatch.data.model

import com.google.firebase.firestore.DocumentId

// The user's public profile (collection users/{uid}).
data class UserProfile(
    @DocumentId
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
)
