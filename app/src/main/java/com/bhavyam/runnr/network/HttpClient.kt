package com.bhavyam.runnr.network
import okhttp3.OkHttpClient


object HttpClient {
    val instance: OkHttpClient = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .build()
}
