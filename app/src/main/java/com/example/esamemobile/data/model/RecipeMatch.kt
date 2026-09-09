package com.example.esamemobile.data.model

// Ricetta abbinata alla dispensa, con l'elenco di cosa manca per prepararla.
data class RecipeMatch(
    val recipe: Recipe,
    val missingIngredients: List<String>,
) {
    val missingCount: Int get() = missingIngredients.size

    // preparabile subito: non manca nulla
    val isReady: Boolean get() = missingIngredients.isEmpty()
}
