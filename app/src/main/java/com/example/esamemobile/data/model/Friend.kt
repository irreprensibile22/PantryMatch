package com.example.esamemobile.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Un amico nella sub-collection `users/{uid}/friends`.
 *
 * Memorizza una copia "denormalizzata" dei dati pubblici dell'amico (nome,
 * email, foto) così da mostrare la lista senza dover leggere ogni profilo, più
 * il momento in cui l'amicizia è stata aggiunta.
 */
data class Friend(
    @DocumentId
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    @ServerTimestamp
    val since: Date? = null,
)
