package com.bhavyam.runnr.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

suspend fun getStreamUrl(encryptedMediaUrl: String, query: String): String? {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val baseUrl = "https://www.jiosaavn.com/api.php"
        val encodedUrl = URLEncoder.encode(encryptedMediaUrl, "UTF-8")

        val finalUrl = "$baseUrl?__call=song.generateAuthToken&url=$encodedUrl&bitrate=320&api_version=4&_format=json&ctx=web6dot0&_marker=0"

        val request = Request.Builder()
            .url(finalUrl)
            .addHeader("User-Agent", "Mozilla/5.0")
            .addHeader("Referer", "https://www.jiosaavn.com/search/song/$query")
            .addHeader("Accept", "application/json")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext null

        val json = JSONObject(body)
        var authUrl = json.optString("auth_url", "")

        if (authUrl.isNotEmpty()) {
            authUrl = authUrl.substringBefore("?")
            authUrl = authUrl.replace("ac.cf.saavncdn.com", "aac.saavncdn.com")
            return@withContext authUrl
        }
        null
    }
}
