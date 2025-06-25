package com.bhavyam.runnr

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
import kotlinx.coroutines.*

class PlaylistFragment : Fragment() {

    private lateinit var playlistTitle: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private var playPause: ImageView? = null
    private var likeBtn: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistTitle = arguments?.getString("playlistName") ?: "Liked Songs"
    }
    @androidx. media3.common. util. UnstableApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_playlist, container, false)

        recyclerView = view.findViewById(R.id.playlistRecycler)
        emptyText = view.findViewById(R.id.emptyMessage)

        val songs = LikedSongsManager.getLikedSongs(requireContext())
        if (songs.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = SearchAdapter { song -> onSongClick(song) }.apply {
                updateList(songs)
            }
        }

        setupPlayerBar()
        return view
    }

    override fun onResume() {
        super.onResume()
        updatePlayerBarUI()
    }
    @androidx. media3.common. util. UnstableApi
    private fun onSongClick(song: SongItem) {
        CoroutineScope(Dispatchers.Main).launch {
            val streamUrl = getStreamUrl(song.encrypted_media_url, song.title)
            if (!streamUrl.isNullOrEmpty()) {
                PlayerManager.setCurrentSong(song)
                PlayerManager.playStream(requireContext(), streamUrl)
                updatePlayerBarUI()
            } else {
                Toast.makeText(requireContext(), "Unable to stream song", Toast.LENGTH_SHORT).show()
            }
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
            if (LikedSongsManager.isLiked(context, currentSong)) {
                LikedSongsManager.removeSong(context, currentSong)
                likeBtn?.setImageResource(R.drawable.ic_heart_outline)
            } else {
                LikedSongsManager.addSong(context, currentSong)
                likeBtn?.setImageResource(R.drawable.ic_heart_filled)
            }
            updatePlaylist()
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

        title?.text = currentSong.title
        subtitle?.text = currentSong.subtitle

        Glide.with(requireContext())
            .asBitmap()
            .load(currentSong.image)
            .into(image)

        updatePlayPauseIcon()

        likeBtn?.setImageResource(
            if (LikedSongsManager.isLiked(requireContext(), currentSong))
                R.drawable.ic_heart_filled
            else
                R.drawable.ic_heart_outline
        )

        colorLayer?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.Richblack))
        nightOverlay?.alpha = 0.75f
        playerBar.visibility = View.VISIBLE
    }

    private fun updatePlayPauseIcon() {
        val isPlaying = PlayerManager.controller?.isPlaying == true
        playPause?.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
    }

    private fun updatePlaylist() {
        val updatedSongs = LikedSongsManager.getLikedSongs(requireContext())
        val adapter = recyclerView.adapter as? SearchAdapter
        adapter?.updateList(updatedSongs)

        if (updatedSongs.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
        }
    }

    companion object {
        fun newInstance(playlistName: String): PlaylistFragment {
            val fragment = PlaylistFragment()
            val args = Bundle()
            args.putString("playlistName", playlistName)
            fragment.arguments = args
            return fragment
        }
    }
}
