package com.example.esamemobile.ui.friends

import com.example.esamemobile.data.model.Friend

/** Un amico con il numero di ricette pubbliche che ha pubblicato. */
data class FriendItem(
    val friend: Friend,
    val recipeCount: Int,
)
