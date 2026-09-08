package com.example.esamemobile.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Risolve un codice a barre (EAN/UPC) nel nome del prodotto usando l'API
 * pubblica e gratuita di Open Food Facts (nessuna API key richiesta).
 *
 * È volutamente basato su [HttpURLConnection] + [org.json] per non introdurre
 * dipendenze HTTP aggiuntive. L'unica operazione di rete gira su
 * [Dispatchers.IO].
 */
class ProductLookupRepository {

    /**
     * @return il nome del prodotto associato al [barcode], oppure `null` se il
     *         prodotto non è presente nel database o non ha un nome.
     * @throws Exception in caso di errore di rete (gestito dal chiamante).
     */
    suspend fun lookupName(barcode: String): String? = withContext(Dispatchers.IO) {
        val url = URL(
            "https://world.openfoodfacts.org/api/v2/product/$barcode.json" +
                "?fields=product_name,product_name_it,brands"
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
            // Preferisci il nome italiano, poi quello generico.
            sequenceOf("product_name_it", "product_name")
                .mapNotNull { product.optString(it).takeIf(String::isNotBlank) }
                .firstOrNull()
        } finally {
            conn.disconnect()
        }
    }
}
