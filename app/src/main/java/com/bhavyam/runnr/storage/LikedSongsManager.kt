package com.bhavyam.runnr.storage

import android.content.Context
import com.bhavyam.runnr.models.SongItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object LikedSongsManager {
    private const val fileName = "liked_songs.json"

    private fun getFile(context: Context): File {
        val dir = File(context.filesDir, "playlists")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, fileName)
    }

    fun getLikedSongs(context: Context): MutableList<SongItem> {
        val file = getFile(context)
        if (!file.exists()) return mutableListOf()
        val json = file.readText()
        return Gson().fromJson(json, object : TypeToken<MutableList<SongItem>>() {}.type)
    }

    fun addSong(context: Context, song: SongItem) {
        val songs = getLikedSongs(context)
        if (songs.none { it.encrypted_media_url == song.encrypted_media_url }) {
            songs.add(song)
            save(context, songs)
        }
    }

    fun removeSong(context: Context, song: SongItem) {
        val songs = getLikedSongs(context)
        songs.removeAll { it.encrypted_media_url == song.encrypted_media_url }
        save(context, songs)
    }

    fun isLiked(context: Context, song: SongItem): Boolean {
        return getLikedSongs(context).any { it.encrypted_media_url == song.encrypted_media_url }
    }

    private fun save(context: Context, songs: List<SongItem>) {
        val file = getFile(context)
        file.writeText(Gson().toJson(songs))
    }fun getAllSongs(context: Context): List<SongItem> {
        val file = File(context.filesDir, "liked_songs.json")
        if (!file.exists()) return emptyList()

        return try {
            val json = file.readText()
            val type = object : TypeToken<List<SongItem>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

}
