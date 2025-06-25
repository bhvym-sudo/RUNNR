package com.bhavyam.runnr.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.bhavyam.runnr.R

class MusicService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null

    companion object {
        private const val CHANNEL_ID = "runnr_music_channel"
        private const val NOTIFICATION_ID = 101
    }

    @UnstableApi
    override fun onCreate() {
        super.onCreate()

        // Create ExoPlayer instance
        player = ExoPlayer.Builder(this).build().apply {
            playWhenReady = true
        }

        // Create MediaSession for media controls
        mediaSession = MediaSession.Builder(this, player)
            .setId("RUNNR_SESSION")
            .build()

        // Start foreground service with notification
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.release()
        player.release()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        player.stop()
        player.release()
        mediaSession?.release()
        mediaSession = null
        stopForeground(true)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }
    @androidx. media3.common. util. UnstableApi
    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RUNNR")
            .setContentText("Music is playing")
            .setSmallIcon(R.drawable.ic_music_note)
            .setStyle(MediaStyle().setMediaSession(mediaSession?.sessionCompatToken))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RUNNR Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
