package com.bhavyam.runnr

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bhavyam.runnr.adapters.SearchAdapter
import com.bhavyam.runnr.models.SongItem
import com.bhavyam.runnr.network.getStreamUrl
import com.bhavyam.runnr.player.PlayerManager
import com.bhavyam.runnr.storage.LikedSongsManager
import com.bhavyam.runnr.PlayerStateListener
import kotlinx.coroutines.*

class LibraryFragment : Fragment(), PlayerStateListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var adapter: SearchAdapter
    private var playPause: ImageView? = null
    private var likeBtn: ImageView? = null

    @androidx.media3.common.util.UnstableApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_library, container, false)

        recyclerView = view.findViewById(R.id.libraryRecycler)
        titleText = view.findViewById(R.id.libraryTitle)
        subtitleText = view.findViewById(R.id.libraryMessage)

        adapter = SearchAdapter { song -> playSong(song) }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        updateList()
        setupPlayerBar()

        return view
    }

    override fun onResume() {
        super.onResume()
        PlayerManager.addListener(this)
        updatePlayerBarUI()
    }

    override fun onPause() {
        super.onPause()
        PlayerManager.removeListener(this)
    }

    private fun updateList() {
        val likedSongs = LikedSongsManager.getLikedSongs(requireContext())
        if (likedSongs.isEmpty()) {
            titleText.text = "Liked Songs"
            subtitleText.text = "No liked songs yet."
            recyclerView.visibility = View.GONE
        } else {
            titleText.text = "Liked Songs"
            subtitleText.text = ""
            adapter.updateList(likedSongs)
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun setupPlayerBar() {
        val playerBar = requireActivity().findViewById<View>(R.id.playerBar) ?: return
        playPause = playerBar.findViewById(R.id.playPauseBtn)
        likeBtn = playerBar.findViewById(R.id.playerLikeBtn)

        playPause?.setOnClickListener {
            val controller = PlayerManager.controller
            if (controller?.isPlaying == true) {
                controller.pause()
            } else {
                controller?.play()
            }
            updatePlayPauseIcon()
        }

        likeBtn?.setOnClickListener {
            val currentSong = PlayerManager.getCurrentSong() ?: return@setOnClickListener
            val context = requireContext()
            val wasLiked = LikedSongsManager.isLiked(context, currentSong)
            if (wasLiked) {
                LikedSongsManager.removeSong(context, currentSong)
                likeBtn?.setImageResource(R.drawable.ic_heart_outline)
            } else {
                LikedSongsManager.addSong(context, currentSong)
                likeBtn?.setImageResource(R.drawable.ic_heart_filled)
            }
            updateList()
        }

        playerBar.setOnClickListener {
            (activity as? MainActivity)?.showFullPlayer()
        }
    }

    private fun updatePlayerBarUI() {
        val playerBar = requireActivity().findViewById<View>(R.id.playerBar) ?: return
        val currentSong = PlayerManager.getCurrentSong() ?: return

        val title = playerBar.findViewById<TextView>(R.id.playerTitle)
        val subtitle = playerBar.findViewById<TextView>(R.id.playerSubtitle)
        val image = playerBar.findViewById<ImageView>(R.id.playerImage)
        val colorLayer = playerBar.findViewById<View>(R.id.colorLayer)
        val nightOverlay = playerBar.findViewById<View>(R.id.nightOverlay)

        playerBar.visibility = View.VISIBLE
        title?.text = currentSong.title
        subtitle?.text = currentSong.subtitle

        Glide.with(requireContext())
            .asBitmap()
            .load(currentSong.image)
            .into(object : com.bumptech.glide.request.target.BitmapImageViewTarget(image) {
                override fun setResource(resource: android.graphics.Bitmap?) {
                    super.setResource(resource)
                    resource?.let {
                        androidx.palette.graphics.Palette.from(it).generate { palette ->
                            val color = palette?.getDominantColor(
                                ContextCompat.getColor(requireContext(), R.color.Richblack)
                            ) ?: ContextCompat.getColor(requireContext(), R.color.Richblack)
                            colorLayer?.setBackgroundColor(color)
                            nightOverlay?.alpha = 0.75f
                        }
                    }
                }
            })

        updatePlayPauseIcon()

        likeBtn?.setImageResource(
            if (LikedSongsManager.isLiked(requireContext(), currentSong))
                R.drawable.ic_heart_filled
            else
                R.drawable.ic_heart_outline
        )
    }

    private fun updatePlayPauseIcon() {
        val isPlaying = PlayerManager.controller?.isPlaying == true
        playPause?.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
    }

    override fun onPlayerStateChanged(isPlaying: Boolean) {
        updatePlayPauseIcon()
    }

    @androidx.media3.common.util.UnstableApi
    private fun playSong(song: SongItem) {
        CoroutineScope(Dispatchers.Main).launch {
            val streamUrl = getStreamUrl(song.encrypted_media_url, song.title)
            if (!streamUrl.isNullOrEmpty()) {
                PlayerManager.setCurrentSong(song)
                PlayerManager.playStream(requireContext(), streamUrl)
                updatePlayerBarUI()
            } else {
                Toast.makeText(requireContext(), "Failed to stream song", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
