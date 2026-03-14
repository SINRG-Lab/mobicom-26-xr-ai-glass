package com.sdk.glassessdksample

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SpotifyAPI() {
    private val BASE_URL = "https://api.spotify.com/v1"

    suspend fun searchTrack(accessToken: String, query: String): String? {
        val url = "$BASE_URL/search?q=${URLEncoder.encode(query, "UTF-8")}&type=track&limit=1"
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.requestMethod = "GET"
        connection.connect()

        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val tracks = json.getJSONObject("tracks").getJSONArray("items")
            if (tracks.length() > 0) {
                val track = tracks.getJSONObject(0)
                return track.getString("uri") // e.g. spotify:track:xxxxx
            }
        }
        return null
    }

    suspend fun playTrack(accessToken: String, uri: String) {
        val url = "https://api.spotify.com/v1/me/player/play"
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.requestMethod = "PUT"
        connection.doOutput = true

        val body = JSONObject().apply {
            put("uris", listOf(uri))
        }
        connection.outputStream.write(body.toString().toByteArray())

        connection.connect()
        connection.inputStream.close()
    }
}
