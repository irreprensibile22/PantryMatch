package com.example.esamemobile.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Risolve un codice a barre interrogando l'API pubblica Open Food Facts (nessuna libreria HTTP extra).
class ProductLookupRepository {

    data class ProductInfo(val name: String?, val category: String?)

    // chiamata di rete: la eseguo su Dispatchers.IO
    suspend fun lookup(barcode: String): ProductInfo? = withContext(Dispatchers.IO) {
        val url = URL(
            "https://world.openfoodfacts.org/api/v2/product/$barcode.json" +
                "?fields=product_name,product_name_it,categories_tags"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("User-Agent", "PantryMatch/1.0 (Android)")
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            if (json.optInt("status") != 1) return@withContext null // status 1 = prodotto trovato
            val product = json.optJSONObject("product") ?: return@withContext null

            // preferisco il nome italiano, altrimenti quello generico
            val name = sequenceOf("product_name_it", "product_name")
                .mapNotNull { product.optString(it).takeIf(String::isNotBlank) }
                .firstOrNull()
            ProductInfo(name = name, category = extractCategory(product))
        } finally {
            conn.disconnect()
        }
    }

    // prendo l'ultima categoria "en:" (la piu specifica) e la ripulisco per usarla come ingrediente
    private fun extractCategory(product: JSONObject): String? {
        val tags = product.optJSONArray("categories_tags") ?: return null
        var chosen: String? = null
        for (i in 0 until tags.length()) {
            val tag = tags.optString(i)
            if (tag.startsWith("en:")) chosen = tag
        }
        return chosen
            ?.removePrefix("en:")
            ?.replace('-', ' ')
            ?.replace('_', ' ')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }
}
