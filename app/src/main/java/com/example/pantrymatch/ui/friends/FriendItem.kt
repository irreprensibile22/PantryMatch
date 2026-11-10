package com.example.pantrymatch.ui.friends

import com.example.pantrymatch.data.model.Friend

// Friend + number of public recipes, so the card can show it without extra queries.
data class FriendItem(
    val friend: Friend,
    val recipeCount: Int,
)
