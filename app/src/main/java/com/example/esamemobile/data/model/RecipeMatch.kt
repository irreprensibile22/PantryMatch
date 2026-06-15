package com.example.esamemobile.data.model

/**
 * Risultato del confronto tra una ricetta e l'inventario dell'utente.
 *
 * @param recipe             la ricetta valutata
 * @param missingIngredients ingredienti (non opzionali) che mancano in dispensa
 */
data class RecipeMatch(
    val recipe: Recipe,
    val missingIngredients: List<String>,
) {
    /** Numero di ingredienti mancanti. */
    val missingCount: Int get() = missingIngredients.size

    /** True se la ricetta è preparabile subito (nessun ingrediente mancante). */
    val isReady: Boolean get() = missingIngredients.isEmpty()
}
