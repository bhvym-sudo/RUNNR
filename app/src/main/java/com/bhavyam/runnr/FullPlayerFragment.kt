package com.bhavyam.runnr

import android.content.ContentValues
import android.graphics.Color
import android.os.*
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
import androidx.media3.common.MediaItem
import android.graphics.drawable.GradientDrawable
import androidx.palette.graphics.Palette
import androidx.core.content.ContextCompat


class FullPlayerFragment : Fragment() {

    private lateinit var image: ImageView
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var playPauseBtn: ImageView
    private lateinit var nextBtn: ImageView
    private lateinit var prevBtn: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView
    private lateinit var likeBtn: ImageView
    private lateinit var repBtn: ImageView
    private lateinit var gradientOverlay: View


    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            PlayerManager.controller?.let { controller ->
                val positionSec = (controller.currentPosition / 1000).toInt()
                seekBar.progress = positionSec
                currentTime.text = formatDuration(positionSec)
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_full_player, container, false)
    }

    @androidx.media3.common.util.UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        image = view.findViewById(R.id.fullPlayerImage)
        title = view.findViewById(R.id.fullPlayerTitle)
        subtitle = view.findViewById(R.id.fullPlayerSubtitle)
        playPauseBtn = view.findViewById(R.id.fullPlayerPlayPause)
        seekBar = view.findViewById(R.id.fullPlayerSeekBar)
        currentTime = view.findViewById(R.id.fullPlayerCurrentTime)
        totalTime = view.findViewById(R.id.fullPlayerTotalTime)
        likeBtn = view.findViewById(R.id.fullPlayerLikeBtn)
        nextBtn = view.findViewById(R.id.nextBtn)
        prevBtn = view.findViewById(R.id.prevBtn)
        repBtn = view.findViewById(R.id.repeatBtn)
        gradientOverlay = view.findViewById(R.id.gradientOverlay)


        val controller = PlayerManager.controller
        if (controller == null) {
            Toast.makeText(requireContext(), "No song is currently playing", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        repBtn.setOnClickListener {
            PlayerManager.isRepeatOn = !PlayerManager.isRepeatOn
            repBtn.setImageResource(
                if (PlayerManager.isRepeatOn) R.drawable.ic_repeat_on else R.drawable.ic_repeat_off
            )
        }
        setupControllerListeners()
        refreshUI()

        playPauseBtn.setOnClickListener {
            if (controller.isPlaying) controller.pause()
            else controller.play()
        }

        nextBtn.setOnClickListener {
            PlayerManager.playNext(requireContext())
        }

        prevBtn.setOnClickListener {
            PlayerManager.playPrevious(requireContext())
        }

        likeBtn.setOnClickListener {
            val currentSong = PlayerManager.getCurrentSong() ?: return@setOnClickListener
            if (LikedSongsManager.isLiked(requireContext(), currentSong)) {
                LikedSongsManager.removeSong(requireContext(), currentSong)
            } else {
                LikedSongsManager.addSong(requireContext(), currentSong)
            }
            updateLikeButton(currentSong)
            updatePlayerBarLikeButton(currentSong)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) controller.seekTo(progress * 1000L)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        view.findViewById<ImageView>(R.id.fullPlayerDownloadBtn)?.setOnClickListener {
            downloadCurrentSong()
        }

        handler.post(updateRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
    }

    private fun refreshUI() {
        if (!isAdded || context == null || view == null) return

        val song = PlayerManager.getCurrentSong() ?: return
        val controller = PlayerManager.controller ?: return

        title.text = song.title
        subtitle.text = song.subtitle

        Glide.with(requireContext())
            .asBitmap()
            .load(song.image)
            .into(object : com.bumptech.glide.request.target.BitmapImageViewTarget(image) {
                override fun setResource(resource: android.graphics.Bitmap?) {
                    super.setResource(resource)
                    resource?.let {
                        Palette.from(it).generate { palette ->
                            val dominantColor = palette?.getDominantColor(
                                ContextCompat.getColor(requireContext(), R.color.black)
                            ) ?: ContextCompat.getColor(requireContext(), R.color.black)

                            val gradient = GradientDrawable(
                                GradientDrawable.Orientation.TOP_BOTTOM,
                                intArrayOf(dominantColor, Color.TRANSPARENT)
                            )
                            gradientOverlay.background = gradient
                        }
                    }
                }
            })

        val durationMs = controller.duration
        val durationSec = (durationMs / 1000).toInt().coerceAtLeast(1)
        seekBar.max = durationSec
        totalTime.text = formatDuration(durationSec)

        playPauseBtn.setImageResource(if (controller.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        updateLikeButton(song)
        updatePlayerBar(song, controller.isPlaying)
        updatePlayerBarLikeButton(song)
    }


    private fun updatePlayerBar(song: SongItem, isPlaying: Boolean) {
        val safeContext = context ?: return
        val playerBar = requireActivity().findViewById<View>(R.id.playerBar)
        val barTitle = playerBar.findViewById<TextView>(R.id.playerTitle)
        val barSubtitle = playerBar.findViewById<TextView>(R.id.playerSubtitle)
        val barImage = playerBar.findViewById<ImageView>(R.id.playerImage)
        val barPlayPause = playerBar.findViewById<ImageView>(R.id.playPauseBtn)

        barTitle.text = song.title
        barSubtitle.text = song.subtitle
        Glide.with(safeContext).load(song.image).into(barImage)
        barPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
    }


    private fun updateLikeButton(song: SongItem) {
        likeBtn.setImageResource(
            if (LikedSongsManager.isLiked(requireContext(), song)) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )
    }

    private fun updatePlayerBarLikeButton(song: SongItem) {
        val safeContext = context ?: return
        val playerBar = requireActivity().findViewById<View>(R.id.playerBar)
        val barLikeBtn = playerBar.findViewById<ImageView>(R.id.playerLikeBtn)
        barLikeBtn?.setImageResource(
            if (LikedSongsManager.isLiked(safeContext, song))
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

    private fun setupControllerListeners() {
        PlayerManager.controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                refreshUI()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                refreshUI()
            }
            @androidx. media3.common. util. UnstableApi
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    val context = context ?: return
                    if (PlayerManager.isRepeatOn) {
                        PlayerManager.controller?.seekTo(0)
                        PlayerManager.controller?.play()
                    } else {
                        PlayerManager.playNext(context)
                    }
                }
            }
        })
    }


    private fun downloadCurrentSong() {
        val song = PlayerManager.getCurrentSong() ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val streamUrl = getStreamUrl(song.encrypted_media_url, song.title)
                if (streamUrl.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        if (isAdded) showToast("Failed to fetch stream URL")
                    }
                    return@launch
                }

                val input = URL(streamUrl).openStream()

                val safeContext = context ?: return@launch
                val resolver = safeContext.contentResolver

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
                    resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                }

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output -> input.copyTo(output) }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }

                    withContext(Dispatchers.Main) {
                        if (isAdded) showToast("Downloaded to Downloads/RUNNR")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        if (isAdded) showToast("Download failed")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    if (isAdded) showToast("Download error")
                }
            }
        }
    }


    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
