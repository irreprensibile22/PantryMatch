package com.example.pantrymatch.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// A user's friend (sub-collection users/{uid}/friends). since = server-side friendship date.
data class Friend(
    @DocumentId
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    @ServerTimestamp
    val since: Date? = null,
)
