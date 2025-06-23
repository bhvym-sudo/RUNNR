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
import com.bhavyam.runnr.PlayerStateListener
import kotlinx.coroutines.*

class PlaylistFragment : Fragment(), PlayerStateListener {

    private lateinit var playlistTitle: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private var playPause: ImageView? = null
    private var likeBtn: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistTitle = arguments?.getString("playlistName") ?: "Liked Songs"
    }

    @androidx.media3.common.util.UnstableApi
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
        PlayerManager.addListener(this)
        updatePlayerBarUI()
    }

    override fun onPause() {
        super.onPause()
        PlayerManager.removeListener(this)
    }

    @androidx.media3.common.util.UnstableApi
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
            val player = PlayerManager.getPlayer()
            if (player?.isPlaying == true) {
                PlayerManager.pause()
            } else {
                PlayerManager.resume()
            }
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
        }

        playerBar.setOnClickListener {
            val fragment = FullPlayerFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
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

        playPause?.setImageResource(
            if (PlayerManager.getPlayer()?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
        )

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

    override fun onPlayerStateChanged(isPlaying: Boolean) {
        val playerBar = requireActivity().findViewById<View>(R.id.playerBar)
        val playPauseBtn = playerBar?.findViewById<ImageView>(R.id.playPauseBtn)
        playPauseBtn?.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
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
