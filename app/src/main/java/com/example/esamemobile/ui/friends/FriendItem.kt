package com.example.esamemobile.ui.friends

import com.example.esamemobile.data.model.Friend

// Amico + numero di ricette pubbliche, cosi la card puo mostrarlo senza altre query.
data class FriendItem(
    val friend: Friend,
    val recipeCount: Int,
)
