package com.bhavyam.runnr

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bhavyam.runnr.adapters.SearchAdapter
import com.bhavyam.runnr.models.SongItem
import com.bhavyam.runnr.network.getStreamUrl
import com.bhavyam.runnr.network.searchJioSaavn
import com.bhavyam.runnr.player.PlayerManager
import com.bhavyam.runnr.storage.LikedSongsManager
import com.bhavyam.runnr.PlayerStateListener
import kotlinx.coroutines.*

class SearchFragment : Fragment(), PlayerStateListener {

    private lateinit var searchAdapter: SearchAdapter
    private var searchJob: Job? = null
    private var playPause: ImageView? = null
    private var likeBtn: ImageView? = null

    @androidx.media3.common.util.UnstableApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        PlayerManager.init(requireContext())
        PlayerManager.addListener(this)

        val searchBox = view.findViewById<EditText>(R.id.searchBox)
        val recyclerView = view.findViewById<RecyclerView>(R.id.searchResults)
        val clearButton = view.findViewById<ImageView>(R.id.clearButton)

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                clearButton.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        clearButton.setOnClickListener {
            searchBox.setText("")
        }

        searchAdapter = SearchAdapter { song -> onSongClick(song) }
        recyclerView.adapter = searchAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(query: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                val text = query.toString().trim()
                if (text.length >= 2) {
                    searchJob = lifecycleScope.launch {
                        delay(150)
                        try {
                            val results = searchJioSaavn(requireContext(), text)
                            searchAdapter.updateList(results)
                        } catch (e: Exception) {
                            Log.e("SearchError", "Error fetching search results", e)
                            Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    searchAdapter.updateList(emptyList())
                }
            }
        })

        return view
    }

    @androidx.media3.common.util.UnstableApi
    private fun onSongClick(song: SongItem) {
        lifecycleScope.launch {
            try {
                val streamUrl = getStreamUrl(song.encrypted_media_url, song.title)
                if (!streamUrl.isNullOrEmpty()) {
                    PlayerManager.setCurrentSong(song)
                    PlayerManager.playStream(requireContext(), streamUrl)
                    updatePlayerBarUI(song)
                } else {
                    Toast.makeText(requireContext(), "Failed to load stream", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Playback error", Toast.LENGTH_SHORT).show()
                Log.e("PlayerManager", "Stream failed", e)
            }
        }
    }

    private fun updatePlayerBarUI(song: SongItem) {
        val activity = requireActivity()
        val playerBar = activity.findViewById<View>(R.id.playerBar) ?: return

        val cardView = playerBar.findViewById<View>(R.id.playerBarCard)
        val title = playerBar.findViewById<TextView>(R.id.playerTitle)
        val subtitle = playerBar.findViewById<TextView>(R.id.playerSubtitle)
        val image = playerBar.findViewById<ImageView>(R.id.playerImage)
        playPause = playerBar.findViewById(R.id.playPauseBtn)
        likeBtn = playerBar.findViewById(R.id.playerLikeBtn)
        val colorLayer = playerBar.findViewById<View>(R.id.colorLayer)
        val nightOverlay = playerBar.findViewById<View>(R.id.nightOverlay)

        playerBar.visibility = View.VISIBLE
        title?.text = song.title
        subtitle?.text = song.subtitle

        Glide.with(requireContext())
            .asBitmap()
            .load(song.image)
            .into(object : com.bumptech.glide.request.target.BitmapImageViewTarget(image) {
                override fun setResource(resource: android.graphics.Bitmap?) {
                    super.setResource(resource)
                    resource?.let {
                        androidx.palette.graphics.Palette.from(it).generate { palette ->
                            val color = palette?.getDominantColor(
                                ContextCompat.getColor(requireContext(), R.color.Richblack)
                            ) ?: ContextCompat.getColor(requireContext(), R.color.Richblack)
                            colorLayer.setBackgroundColor(color)
                            nightOverlay.alpha = 0.75f
                        }
                    }
                }
            })

        playPause?.setImageResource(R.drawable.ic_pause)
        playPause?.setOnClickListener {
            val player = PlayerManager.getPlayer()
            if (player?.isPlaying == true) {
                PlayerManager.pause()
            } else {
                PlayerManager.resume()
            }
        }

        updateLikeIcon(song)
        likeBtn?.setOnClickListener {
            val liked = LikedSongsManager.isLiked(requireContext(), song)
            if (liked) {
                LikedSongsManager.removeSong(requireContext(), song)
            } else {
                LikedSongsManager.addSong(requireContext(), song)
            }
            updateLikeIcon(song)
        }

        playerBar.setOnClickListener {
            val fragment = FullPlayerFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun updateLikeIcon(song: SongItem) {
        likeBtn?.setImageResource(
            if (LikedSongsManager.isLiked(requireContext(), song))
                R.drawable.ic_heart_filled
            else
                R.drawable.ic_heart_outline
        )
    }

    override fun onPlayerStateChanged(isPlaying: Boolean) {
        val playerBar = requireActivity().findViewById<View>(R.id.playerBar)
        val playPauseBtn = playerBar?.findViewById<ImageView>(R.id.playPauseBtn)
        playPauseBtn?.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        PlayerManager.removeListener(this)
    }
}
