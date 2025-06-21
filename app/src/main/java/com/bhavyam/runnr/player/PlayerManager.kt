package com.bhavyam.runnr.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.bhavyam.runnr.models.SongItem

import com.bhavyam.runnr.PlayerStateListener

object PlayerManager {
    private var player: ExoPlayer? = null

    fun init(context: Context) {
        if (player == null) {
            player = ExoPlayer.Builder(context).build()
        }
    }

    fun playStream(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
        notifyListeners(true)
    }

    fun pause() {
        player?.pause()
        notifyListeners(false)
    }

    fun resume() {
        player?.play()
        notifyListeners(true)
    }

    fun getPlayer(): ExoPlayer? = player

    fun release() {
        player?.release()
        player = null
    }
    private var currentSong: SongItem? = null

    fun setCurrentSong(song: SongItem) {
        currentSong = song
    }

    fun getCurrentSong(): SongItem? = currentSong
    private val listeners = mutableListOf<PlayerStateListener>()

    fun addListener(listener: PlayerStateListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: PlayerStateListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(isPlaying: Boolean) {
        listeners.forEach { it.onPlayerStateChanged(isPlaying) }
    }
}
