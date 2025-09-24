package com.bhavyam.runnr.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bhavyam.runnr.PlaybackQueueManager
import com.bhavyam.runnr.PlayerStateListener
import com.bhavyam.runnr.models.SongItem
import com.bhavyam.runnr.network.getStreamUrl
import com.bhavyam.runnr.service.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PlayerManager {
    var controller: MediaController? = null
    var isRepeatOn = false
    private var currentSong: SongItem? = null
    private val listeners = mutableListOf<PlayerStateListener>()

    fun initController(context: Context, onReady: (() -> Unit)? = null) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java)
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

    fun addListener(listener: PlayerStateListener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: PlayerStateListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(isPlaying: Boolean) {
        listeners.forEach { it.onPlayerStateChanged(isPlaying) }
    }

    @UnstableApi
    fun playStream(context: Context, song: SongItem) {
        CoroutineScope(Dispatchers.Main).launch {
            val streamUrl = getStreamUrl(song.encrypted_media_url, song.title)
            if (!streamUrl.isNullOrEmpty()) {
                val intent = Intent(context, MusicService::class.java)
                context.startForegroundService(intent)

                controller?.let { ctrl ->
                    val mediaItem = MediaItem.fromUri(streamUrl)
                    ctrl.setMediaItem(mediaItem)
                    ctrl.prepare()
                    ctrl.play()
                    notifyListeners(true)
                    setCurrentSong(song)
                } ?: run {
                    initController(context) {
                        playStream(context, song)
                    }
                }
            } else {
                Toast.makeText(context, "Unable to stream song", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getCurrentSong(): SongItem? = currentSong

    @UnstableApi
    fun playSongWithQueue(context: Context, songList: List<SongItem>, songIndex: Int) {
        PlaybackQueueManager.setQueue(songList, songIndex)
        val song = songList[songIndex]
        setCurrentSong(song)
        playStream(context, song)
    }

    @UnstableApi
    fun playNext(context: Context) {
        val nextSong = PlaybackQueueManager.getNextSong() ?: return
        setCurrentSong(nextSong)
        playStream(context, nextSong)
    }

    @UnstableApi
    fun playPrevious(context: Context) {
        val previousSong = PlaybackQueueManager.getPreviousSong() ?: return
        setCurrentSong(previousSong)
        playStream(context, previousSong)
    }

    fun pause() {
        controller?.pause()
        notifyListeners(false)
    }

    fun resume() {
        controller?.play()
        notifyListeners(true)
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
    }

    fun getCurrentPosition(): Long {
        return controller?.currentPosition ?: 0L
    }

    fun getDuration(): Long {
        return controller?.duration ?: 0L
    }

    fun isPlaying(): Boolean {
        return controller?.isPlaying ?: false
    }
}