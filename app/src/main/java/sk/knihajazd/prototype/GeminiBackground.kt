package sk.knihajazd.prototype

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Calls an authenticated backend, NEVER the Gemini API with an embedded developer key. */
object GeminiBackground {
    suspend fun generate(
        baseUrl: String, accessToken: String, brand: String, model: String,
        year: String, color: String, finish: String, destination: File
    ): String = withContext(Dispatchers.IO) {
        val base = baseUrl.trim().trimEnd('/')
        require(base.startsWith("https://")) { "Vyžaduje sa HTTPS adresa servera." }
        require(accessToken.isNotBlank()) { "Chýba prístupový token servera." }
        val connection = URL("$base/generate").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 120000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            val request = JSONObject()
                .put("brand", brand).put("model", model).put("year", year)
                .put("color", color).put("finish", finish)
                .toString()
            connection.outputStream.use { it.write(request.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) {
                val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("Server HTTP $status: ${body.take(180)}")
            }
            val type = connection.contentType.orEmpty()
            require(type.startsWith("image/")) { "Server nevrátil obrázok." }
            val temp = File(destination.parentFile, destination.name + ".tmp")
            connection.inputStream.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            if (temp.length() < 10_000 || temp.length() > 22_000_000) {
                temp.delete()
                throw IllegalStateException("Obrázok má neplatnú veľkosť.")
            }
            if (destination.exists()) destination.delete()
            check(temp.renameTo(destination)) { "Nepodarilo sa uložiť obrázok." }
            destination.absolutePath
        } finally {
            connection.disconnect()
        }
    }
}
