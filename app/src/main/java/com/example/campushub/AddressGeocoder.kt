package com.example.campushub

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Converte um endereço em coordenadas usando o OpenStreetMap (Nominatim). */
object AddressGeocoder {

    data class Point(val latitude: Double, val longitude: Double, val displayName: String)

    fun search(address: String, onResult: (Point?) -> Unit) {
        val mainThread = Handler(Looper.getMainLooper())
        Thread {
            val point = try {
                request(address)
            } catch (e: Exception) {
                null
            }
            mainThread.post { onResult(point) }
        }.start()
    }

    private fun request(address: String): Point? {
        val query = URLEncoder.encode(address, "UTF-8")
        val url = URL("https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=br&q=$query")
        val connection = url.openConnection() as HttpURLConnection
        try {
            // A política de uso do Nominatim exige um User-Agent que identifique o app
            connection.setRequestProperty("User-Agent", "CampusHub/1.0 (projeto academico)")
            connection.setRequestProperty("Accept-Language", "pt-BR")
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val results = JSONArray(body)
            if (results.length() == 0) return null

            val first = results.getJSONObject(0)
            return Point(
                first.getString("lat").toDouble(),
                first.getString("lon").toDouble(),
                first.optString("display_name")
            )
        } finally {
            connection.disconnect()
        }
    }
}