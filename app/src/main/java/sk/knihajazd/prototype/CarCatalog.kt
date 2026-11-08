package sk.knihajazd.prototype

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale

/**
 * NHTSA vPIC open vehicle catalog (makes and models). Not exhaustive for every
 * market/year. The UI retains a manual make/model fallback.
 */
object CarCatalog {
    suspend fun fetchMakes(): List<String> = withContext(Dispatchers.IO) {
        readResults("https://vpic.nhtsa.dot.gov/api/vehicles/GetAllMakes?format=json", "Make_Name")
    }

    suspend fun fetchModels(make: String): List<String> = withContext(Dispatchers.IO) {
        val normalized = Normalizer.normalize(make, Normalizer.Form.NFD)
            .replace("\\p{Mn}".toRegex(), "")
            .replace("Mercedes-Benz", "Mercedes Benz", ignoreCase = true)
        val encoded = URLEncoder.encode(normalized, "UTF-8")
        val url = "https://vpic.nhtsa.dot.gov/api/vehicles/GetModelsForMake/$encoded?format=json"
        val json = requestJson(url)
        val result = json.optJSONArray("Results") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until result.length()) {
                val entry = result.optJSONObject(i) ?: continue
                val brandName = entry.optString("Make_Name")
                val modelName = entry.optString("Model_Name").trim()
                if (modelName.isNotBlank() &&
                    (brandName.equals(normalized, ignoreCase = true) ||
                     brandName.equals(make, ignoreCase = true))) {
                    add(modelName)
                }
            }
        }.distinctBy { it.lowercase(Locale.ROOT) }.sortedBy { it.lowercase(Locale.ROOT) }
    }

    private fun readResults(url: String, key: String): List<String> {
        val array = requestJson(url).optJSONArray("Results") ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val value = array.optJSONObject(i)?.optString(key)?.trim().orEmpty()
                if (value.isNotEmpty()) add(value)
            }
        }.distinctBy { it.lowercase(Locale.ROOT) }.sortedBy { it.lowercase(Locale.ROOT) }
    }

    private fun requestJson(address: String): JSONObject {
        val connection = URL(address).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 12000
            connection.readTimeout = 12000
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Catalog HTTP ${connection.responseCode}")
            }
            val payload = connection.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(payload)
        } finally {
            connection.disconnect()
        }
    }
}
