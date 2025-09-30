package com.bhavyam.runnr.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
    private var currentQueue: List<SongItem> = emptyList()

    @OptIn(UnstableApi::class)
    fun initController(context: Context, onReady: (() -> Unit)? = null) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java)
        )

        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get()
            controller?.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    updateCurrentSongFromQueue()
                    notifyListeners(controller?.isPlaying == true)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    notifyListeners(isPlaying)
                }
            })
            onReady?.invoke()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun updateCurrentSongFromQueue() {
        controller?.let { ctrl ->
            val currentIndex = ctrl.currentMediaItemIndex
            if (currentIndex >= 0 && currentIndex < currentQueue.size) {
                currentSong = currentQueue[currentIndex]
                PlaybackQueueManager.setCurrentIndex(currentIndex)
            }
        }
    }

    fun setCurrentSong(song: SongItem) {
        currentSong = song
        notifyListeners(isPlaying())
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
        setCurrentSong(song)
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
        currentQueue = songList
        PlaybackQueueManager.setQueue(songList, songIndex)
        setCurrentSong(songList[songIndex])

        CoroutineScope(Dispatchers.Main).launch {
            val mediaItems = mutableListOf<MediaItem>()

            for (song in songList) {
                val streamUrl = getStreamUrl(song.encrypted_media_url, song.title)
                if (!streamUrl.isNullOrEmpty()) {
                    mediaItems.add(MediaItem.fromUri(streamUrl))
                }
            }

            if (mediaItems.isNotEmpty()) {
                val intent = Intent(context, MusicService::class.java)
                context.startForegroundService(intent)

                controller?.let { ctrl ->
                    ctrl.setMediaItems(mediaItems, songIndex, 0)
                    ctrl.prepare()
                    ctrl.play()
                    notifyListeners(true)
                } ?: run {
                    initController(context) {
                        playSongWithQueue(context, songList, songIndex)
                    }
                }
            } else {
                Toast.makeText(context, "Unable to load songs", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @UnstableApi
    fun playNext(context: Context) {
        controller?.let { ctrl ->
            if (ctrl.hasNextMediaItem()) {
                ctrl.seekToNextMediaItem()
            } else {
                val nextSong = PlaybackQueueManager.getNextSong() ?: return
                setCurrentSong(nextSong)
                playStream(context, nextSong)
            }
        }
    }

    @UnstableApi
    fun playPrevious(context: Context) {
        controller?.let { ctrl ->
            if (ctrl.hasPreviousMediaItem()) {
                ctrl.seekToPreviousMediaItem()
            } else {
                val previousSong = PlaybackQueueManager.getPreviousSong() ?: return
                setCurrentSong(previousSong)
                playStream(context, previousSong)
            }
        }
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
