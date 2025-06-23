package com.bhavyam.runnr.network

import android.content.Context
import android.widget.Toast
import com.bhavyam.runnr.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

suspend fun searchJioSaavn(context: Context, query: String): List<SongItem> {
    val client = HttpClient.instance
    val encodedQuery = URLEncoder.encode(query, "UTF-8")
    val url = "https://www.jiosaavn.com/api.php?p=1&q=$encodedQuery&_format=json&_marker=0&api_version=4&ctx=web6dot0&n=20&__call=search.getResults"

    val request = Request.Builder()
        .url(url)
        .addHeader("User-Agent", "Mozilla/5.0")
        .addHeader("Referer", "https://www.jiosaavn.com/search/song/$query")
        .addHeader("Accept", "application/json")
        .addHeader("Connection", "keep-alive")
        .build()

    return withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            val songList = mutableListOf<SongItem>()

            if (response.isSuccessful && body != null) {
                val json = JSONObject(body)
                val results = json.optJSONArray("results")

                if (results != null) {
                    for (i in 0 until results.length()) {
                        val item = results.getJSONObject(i)
                        val info = item.optJSONObject("more_info") ?: JSONObject()

                        val song = SongItem(
                            title = item.optString("title"),
                            subtitle = item.optString("subtitle"),
                            image = item.optString("image"),
                            duration = info.optString("duration"),
                            encrypted_media_url = info.optString("encrypted_media_url")
                        )
                        songList.add(song)
                    }
                }
            } else {
                throw IOException("Network error: ${response.code}")
            }

            songList
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to fetch songs", Toast.LENGTH_SHORT).show()
            throw IOException("Failed to fetch songs", e)
        }
    }
}
