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
import androidx.media3.common.util.UnstableApi
import androidx.palette.graphics.Palette
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bhavyam.runnr.models.SongItem
import com.bhavyam.runnr.network.getStreamUrl
import com.bhavyam.runnr.player.PlayerManager
import com.bhavyam.runnr.storage.LikedSongsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import android.graphics.drawable.GradientDrawable
import androidx.annotation.OptIn

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

    private var isUserSeeking = false
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateSeekBar()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_full_player, container, false)
    }

    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViews(view)

        val controller = PlayerManager.controller
        if (controller == null) {
            Toast.makeText(requireContext(), "No song is currently playing", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        setupClickListeners()
        setupControllerListeners()
        refreshUI()
        startSeekBarUpdates()
    }

    private fun initViews(view: View) {
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
    }

    @OptIn(UnstableApi::class)
    private fun setupClickListeners() {
        repBtn.setOnClickListener {
            PlayerManager.isRepeatOn = !PlayerManager.isRepeatOn
            repBtn.setImageResource(
                if (PlayerManager.isRepeatOn) R.drawable.ic_repeat_on else R.drawable.ic_repeat_off
            )
        }

        playPauseBtn.setOnClickListener {
            val controller = PlayerManager.controller ?: return@setOnClickListener
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }

        nextBtn.setOnClickListener { PlayerManager.playNext(requireContext()) }
        prevBtn.setOnClickListener { PlayerManager.playPrevious(requireContext()) }

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
                if (fromUser) {
                    currentTime.text = formatDuration(progress)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {
                isUserSeeking = true
            }
            override fun onStopTrackingTouch(sb: SeekBar?) {
                isUserSeeking = false
                val controller = PlayerManager.controller ?: return
                controller.seekTo(sb?.progress?.times(1000L) ?: 0L)
            }
        })

        view?.findViewById<ImageView>(R.id.fullPlayerDownloadBtn)?.setOnClickListener {
            downloadCurrentSong()
        }
    }

    private fun startSeekBarUpdates() {
        handler.post(updateRunnable)
    }

    private fun stopSeekBarUpdates() {
        handler.removeCallbacks(updateRunnable)
    }

    private fun updateSeekBar() {
        if (isUserSeeking || !isAdded) return

        val controller = PlayerManager.controller ?: return
        val duration = controller.duration
        val position = controller.currentPosition

        if (duration > 0) {
            val durationSec = (duration / 1000).toInt()
            val positionSec = (position / 1000).toInt()

            seekBar.max = durationSec
            seekBar.progress = positionSec

            currentTime.text = formatDuration(positionSec)
            totalTime.text = formatDuration(durationSec)
        }
    }

    private fun refreshUI() {
        if (!isAdded) return

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
                            if (isAdded) {
                                val vibrantColor = palette?.getVibrantColor(
                                    ContextCompat.getColor(requireContext(), R.color.black)
                                )
                                val lightVibrantColor = palette?.getLightVibrantColor(
                                    ContextCompat.getColor(requireContext(), R.color.black)
                                )
                                val mutedColor = palette?.getMutedColor(
                                    ContextCompat.getColor(requireContext(), R.color.black)
                                )

                                val brightestColor = when {
                                    lightVibrantColor != null -> lightVibrantColor
                                    vibrantColor != null -> vibrantColor
                                    mutedColor != null -> mutedColor
                                    else -> ContextCompat.getColor(requireContext(), R.color.black)
                                }

                                val gradient = GradientDrawable(
                                    GradientDrawable.Orientation.TOP_BOTTOM,
                                    intArrayOf(brightestColor, Color.TRANSPARENT)
                                )
                                gradientOverlay.background = gradient
                            }
                        }
                    }
                }
            })

        playPauseBtn.setImageResource(if (controller.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        updateLikeButton(song)
        updatePlayerBar(song, controller.isPlaying)
        updatePlayerBarLikeButton(song)
        updateSeekBar()
    }

    private var playerListener: Player.Listener? = null

    private fun setupControllerListeners() {
        playerListener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (isAdded) {
                    refreshUI()
                }
            }

            @OptIn(UnstableApi::class)
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

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isAdded) {
                    playPauseBtn.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
                    PlayerManager.getCurrentSong()?.let { song ->
                        updatePlayerBar(song, isPlaying)
                    }
                }
            }
        }

        playerListener?.let { PlayerManager.controller?.addListener(it) }
    }

    private fun updatePlayerBar(song: SongItem, isPlaying: Boolean) {
        if (!isAdded) return

        val playerBar = requireActivity().findViewById<View>(R.id.playerBar)
        val barTitle = playerBar?.findViewById<TextView>(R.id.playerTitle)
        val barSubtitle = playerBar?.findViewById<TextView>(R.id.playerSubtitle)
        val barImage = playerBar?.findViewById<ImageView>(R.id.playerImage)
        val barPlayPause = playerBar?.findViewById<ImageView>(R.id.playPauseBtn)

        barTitle?.text = song.title
        barSubtitle?.text = song.subtitle
        barImage?.let { Glide.with(requireContext()).load(song.image).into(it) }
        barPlayPause?.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
    }

    private fun updateLikeButton(song: SongItem) {
        likeBtn.setImageResource(
            if (LikedSongsManager.isLiked(requireContext(), song)) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )
    }

    private fun updatePlayerBarLikeButton(song: SongItem) {
        val playerBar = requireActivity().findViewById<View>(R.id.playerBar)
        val barLikeBtn = playerBar?.findViewById<ImageView>(R.id.playerLikeBtn)
        barLikeBtn?.setImageResource(
            if (LikedSongsManager.isLiked(requireContext(), song)) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )
    }

    private fun formatDuration(seconds: Int): String {
        val min = seconds / 60
        val sec = seconds % 60
        return "%d:%02d".format(min, sec)
    }

    private fun downloadCurrentSong() {
        val song = PlayerManager.getCurrentSong() ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val streamUrl = getStreamUrl(song.encrypted_media_url, song.title)
                if (streamUrl.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) { showToast("Failed to fetch stream URL") }
                    return@launch
                }
                val input = URL(streamUrl).openStream()
                val resolver = requireContext().contentResolver
                val fileName = "${song.title}-${System.currentTimeMillis()}.mp3"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/RUNNR")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                else resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(uri)?.use { output -> input.copyTo(output) }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                    withContext(Dispatchers.Main) { showToast("Downloaded to Downloads/RUNNR") }
                } ?: withContext(Dispatchers.Main) { showToast("Download failed") }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { showToast("Download error") }
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        startSeekBarUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopSeekBarUpdates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopSeekBarUpdates()
        playerListener?.let { PlayerManager.controller?.removeListener(it) }
        playerListener = null
    }
}