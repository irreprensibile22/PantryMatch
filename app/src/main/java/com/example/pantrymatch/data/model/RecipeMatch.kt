package com.example.pantrymatch.data.model

// A recipe matched against the pantry, with the list of what is missing to cook it.
data class RecipeMatch(
    val recipe: Recipe,
    val missingIngredients: List<String>,
) {
    val missingCount: Int get() = missingIngredients.size

    // ready to cook: nothing is missing
    val isReady: Boolean get() = missingIngredients.isEmpty()
}
