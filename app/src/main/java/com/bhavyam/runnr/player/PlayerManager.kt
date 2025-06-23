package com.bhavyam.runnr.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.bhavyam.runnr.models.SongItem
import com.bhavyam.runnr.PlayerStateListener
import android.content.Intent
import com.bhavyam.runnr.service.MusicService

object PlayerManager {
    private var player: ExoPlayer? = null

    fun init(context: Context) {
        if (player == null) {
            player = ExoPlayer.Builder(context.applicationContext).build()
        }
    }
    @androidx.media3.common.util.UnstableApi
    fun playStream(context: Context, url: String) {
        val intent = Intent(context, MusicService::class.java)
        context.startForegroundService(intent)
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(5000)
            .setReadTimeoutMs(10000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Mozilla/5.0")

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(url))

        player?.setMediaSource(mediaSource)
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
