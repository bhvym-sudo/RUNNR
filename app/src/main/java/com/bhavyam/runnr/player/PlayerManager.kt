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
import com.bhavyam.runnr.PlaybackQueueManager
import com.bhavyam.runnr.PlayerStateListener
import android.widget.Toast
import androidx.media3.common.util.UnstableApi
import com.bhavyam.runnr.network.getStreamUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch



object PlayerManager {
    var controller: MediaController? = null
    var isRepeatOn = false
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
    fun playStream(context: Context, song: SongItem) {
        CoroutineScope(Dispatchers.Main).launch {
            val streamUrl = getStreamUrl(song.encrypted_media_url, song.title)
            if (!streamUrl.isNullOrEmpty()) {
                val intent = Intent(context, MusicService::class.java)
                context.startForegroundService(intent)

                val controller = controller ?: return@launch

                val mediaItem = MediaItem.Builder()
                    .setUri(streamUrl)
                    .build()

                controller.setMediaItem(mediaItem)
                controller.prepare()
                controller.play()

                notifyListeners(true)
            } else {
                Toast.makeText(context, "Unable to stream song", Toast.LENGTH_SHORT).show()
            }
        }
    }




    fun getCurrentSong(): SongItem? = currentSong

    @androidx. media3.common. util. UnstableApi
    fun playSongWithQueue(context: Context, songList: List<SongItem>, songIndex: Int) {
        PlaybackQueueManager.setQueue(songList, songIndex)
        val song = songList[songIndex]
        setCurrentSong(song)
        playStream(context, song)
    }

    @androidx. media3.common. util. UnstableApi
    fun playNext(context: Context) {
        val nextSong = PlaybackQueueManager.getNextSong() ?: return
        setCurrentSong(nextSong)
        playStream(context, nextSong)
    }

    @androidx. media3.common. util. UnstableApi
    fun playPrevious(context: Context) {
        val previousSong = PlaybackQueueManager.getPreviousSong() ?: return
        setCurrentSong(previousSong)
        playStream(context, previousSong)
    }


}
