package com.example.esamemobile.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Risolve un codice a barre (EAN/UPC) in informazioni sul prodotto usando l'API
 * pubblica e gratuita di Open Food Facts (nessuna API key richiesta).
 *
 * È volutamente basato su [HttpURLConnection] + [org.json] per non introdurre
 * dipendenze HTTP aggiuntive. L'unica operazione di rete gira su
 * [Dispatchers.IO].
 */
class ProductLookupRepository {

    /**
     * Dati utili di un prodotto.
     *
     * @param name     nome commerciale (es. "Nutella", "Arrabbiata")
     * @param category categoria ripulita (es. "cooking sauces") da usare come
     *                 nome ingrediente di default; spesso in inglese perché
     *                 Open Food Facts non fornisce l'italiano in modo affidabile.
     */
    data class ProductInfo(val name: String?, val category: String?)

    /**
     * @return le info del prodotto, oppure `null` se il prodotto non è presente
     *         nel database Open Food Facts.
     * @throws Exception in caso di errore di rete (gestito dal chiamante).
     */
    suspend fun lookup(barcode: String): ProductInfo? = withContext(Dispatchers.IO) {
        val url = URL(
            "https://world.openfoodfacts.org/api/v2/product/$barcode.json" +
                "?fields=product_name,product_name_it,categories_tags"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            // Open Food Facts raccomanda uno User-Agent identificativo.
            setRequestProperty("User-Agent", "PantryMatch/1.0 (Android)")
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            // status == 1 => prodotto trovato.
            if (json.optInt("status") != 1) return@withContext null
            val product = json.optJSONObject("product") ?: return@withContext null

            val name = sequenceOf("product_name_it", "product_name")
                .mapNotNull { product.optString(it).takeIf(String::isNotBlank) }
                .firstOrNull()
            ProductInfo(name = name, category = extractCategory(product))
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Sceglie la categoria più specifica in inglese (ultima voce `en:` della
     * gerarchia `categories_tags`) e la ripulisce in testo leggibile.
     * Le voci in cima alla gerarchia sono macro-categorie inutili come
     * "breakfasts"/"beverages", quindi si preferisce quella più in basso.
     */
    private fun extractCategory(product: JSONObject): String? {
        val tags = product.optJSONArray("categories_tags") ?: return null
        var chosen: String? = null
        for (i in 0 until tags.length()) {
            val tag = tags.optString(i)
            if (tag.startsWith("en:")) chosen = tag  // tieni l'ultima "en:"
        }
        return chosen
            ?.removePrefix("en:")
            ?.replace('-', ' ')
            ?.replace('_', ' ')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }
}
