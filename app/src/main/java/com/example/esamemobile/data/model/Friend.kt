package com.example.esamemobile.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Amico dell'utente (sotto-collezione users/{uid}/friends). since = data amicizia lato server.
data class Friend(
    @DocumentId
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    @ServerTimestamp
    val since: Date? = null,
)
