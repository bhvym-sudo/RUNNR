package com.bhavyam.runnr

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.common.Player

import com.bumptech.glide.Glide
import com.bhavyam.runnr.models.SongItem
import com.bhavyam.runnr.network.getStreamUrl
import com.bhavyam.runnr.player.PlayerManager
import com.bhavyam.runnr.storage.LikedSongsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class FullPlayerFragment : Fragment() {

    private lateinit var image: ImageView
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var playPauseBtn: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView
    private lateinit var likeBtn: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            val controller = PlayerManager.controller
            if (controller != null && controller.isPlaying) {
                val positionSec = (controller.currentPosition / 1000).toInt()
                seekBar.progress = positionSec
                currentTime.text = formatDuration(positionSec)
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_full_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        image = view.findViewById(R.id.fullPlayerImage)
        title = view.findViewById(R.id.fullPlayerTitle)
        subtitle = view.findViewById(R.id.fullPlayerSubtitle)
        playPauseBtn = view.findViewById(R.id.fullPlayerPlayPause)
        seekBar = view.findViewById(R.id.fullPlayerSeekBar)
        currentTime = view.findViewById(R.id.fullPlayerCurrentTime)
        totalTime = view.findViewById(R.id.fullPlayerTotalTime)
        likeBtn = view.findViewById(R.id.fullPlayerLikeBtn)

        val controller = PlayerManager.controller
        val song = PlayerManager.getCurrentSong()

        if (controller == null || song == null) {
            Toast.makeText(requireContext(), "No song is currently playing", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                activity?.runOnUiThread {
                    playPauseBtn.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
                    val playerBar = requireActivity().findViewById<View>(R.id.playerBar)
                    val playPause = playerBar?.findViewById<ImageView>(R.id.playPauseBtn)
                    playPause?.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
                }
            }
        })

        updateLikeButton(song)

        likeBtn.setOnClickListener {
            if (LikedSongsManager.isLiked(requireContext(), song)) {
                LikedSongsManager.removeSong(requireContext(), song)
            } else {
                LikedSongsManager.addSong(requireContext(), song)
            }
            updateLikeButton(song)
            updatePlayerBarLikeButton(song)
        }

        val downloadBtn = view.findViewById<ImageView>(R.id.fullPlayerDownloadBtn)
        downloadBtn.setOnClickListener {
            downloadCurrentSong()
        }

        title.text = song.title
        subtitle.text = song.subtitle
        Glide.with(requireContext()).load(song.image).into(image)

        val durationMs = controller.duration
        val durationSec = (durationMs / 1000).toInt().coerceAtLeast(1)
        seekBar.max = durationSec
        totalTime.text = formatDuration(durationSec)

        playPauseBtn.setImageResource(if (controller.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)

        playPauseBtn.setOnClickListener {
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    controller.seekTo(progress * 1000L)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        handler.post(updateRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
    }

    private fun updateLikeButton(song: SongItem) {
        val isLiked = LikedSongsManager.isLiked(requireContext(), song)
        likeBtn.setImageResource(if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
    }

    private fun updatePlayerBarLikeButton(song: SongItem) {
        val playerBar = requireActivity().findViewById<View>(R.id.playerBar)
        val playerLikeBtn = playerBar?.findViewById<ImageView>(R.id.playerLikeBtn)
        playerLikeBtn?.setImageResource(
            if (LikedSongsManager.isLiked(requireContext(), song))
                R.drawable.ic_heart_filled
            else
                R.drawable.ic_heart_outline
        )
    }

    private fun formatDuration(seconds: Int): String {
        val min = seconds / 60
        val sec = seconds % 60
        return String.format("%d:%02d", min, sec)
    }

    private fun downloadCurrentSong() {
        val song = PlayerManager.getCurrentSong() ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val streamUrl = getStreamUrl(song.encrypted_media_url, song.title)
                if (streamUrl.isNullOrEmpty()) {
                    showToast("Failed to fetch stream URL")
                    return@launch
                }

                val input = URL(streamUrl).openStream()
                val resolver = requireContext().contentResolver

                val fileName = "${song.title}-${System.currentTimeMillis()}.mp3"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/RUNNR")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                } else {
                    MediaStore.Files.getContentUri("external").let {
                        resolver.insert(it, contentValues)
                    }
                }

                uri?.let {
                    resolver.openOutputStream(it)?.use { output -> input.copyTo(output) }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(it, contentValues, null, null)
                    }

                    withContext(Dispatchers.Main) {
                        showToast("Downloaded to Downloads/RUNNR")
                    }
                } ?: showToast("Download failed")

            } catch (e: Exception) {
                showToast("Download error")
                e.printStackTrace()
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
