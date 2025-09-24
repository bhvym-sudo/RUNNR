package com.bhavyam.runnr.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

suspend fun getStreamUrl(encryptedMediaUrl: String, query: String? = null): String? {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val encodedUrl = URLEncoder.encode(encryptedMediaUrl, "UTF-8")
        val finalUrl = "https://www.jiosaavn.com/api.php?__call=song.generateAuthToken&url=$encodedUrl&bitrate=320&api_version=4&_format=json&ctx=web6dot0&_marker=0"

        val request = Request.Builder()
            .url(finalUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138 Safari/537.36")
            .addHeader("Referer", "https://www.jiosaavn.com/")
            .addHeader("Origin", "https://www.jiosaavn.com")
            .addHeader("Accept", "application/json")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext null
        val json = JSONObject(body)
        json.optString("auth_url", null)
    }
}
