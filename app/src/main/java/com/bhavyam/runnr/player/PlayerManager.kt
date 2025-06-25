package com.bhavyam.runnr.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bhavyam.runnr.models.SongItem
import com.bhavyam.runnr.service.MusicService
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.bhavyam.runnr.PlayerStateListener


object PlayerManager {
    var controller: MediaController? = null
    private var exoPlayer: ExoPlayer? = null
    private var currentSong: SongItem? = null

    fun initController(context: Context, onReady: (() -> Unit)? = null) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, com.bhavyam.runnr.service.MusicService::class.java)
        )

        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get()
            onReady?.invoke()
        }, ContextCompat.getMainExecutor(context))
    }


    fun setCurrentSong(song: SongItem) {
        currentSong = song
    }
    private val listeners = mutableListOf<PlayerStateListener>()

    fun addListener(listener: PlayerStateListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: PlayerStateListener) {
        listeners.remove(listener)
    }
    private fun notifyListeners(isPlaying: Boolean) {
        listeners.forEach { it.onPlayerStateChanged(isPlaying) }
    }


    @androidx.media3.common.util.UnstableApi
    fun playStream(context: Context, streamUrl: String) {
        val intent = Intent(context, MusicService::class.java)
        context.startForegroundService(intent)

        val controller = controller ?: return

        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.setRepeatMode(androidx.media3.common.Player.REPEAT_MODE_ONE) // remove this to remove repeat

        controller.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                notifyListeners(isPlaying)
                controller.removeListener(this)
            }
        })

        controller.play()
    }



    fun getCurrentSong(): SongItem? = currentSong
}
