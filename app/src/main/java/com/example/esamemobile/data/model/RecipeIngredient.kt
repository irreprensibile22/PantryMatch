package com.example.esamemobile.data.model

/**
 * Un ingrediente all'interno di una ricetta.
 *
 * @param name       nome leggibile dell'ingrediente (es. "Pomodoro")
 * @param quantity   quantità opzionale (es. 200.0)
 * @param unit       unità di misura opzionale (es. "g", "ml", "pz")
 * @param optional   se true l'ingrediente non viene conteggiato tra i mancanti
 */
data class RecipeIngredient(
    val name: String = "",
    val quantity: Double? = null,
    val unit: String? = null,
    val optional: Boolean = false,
)
