package com.bhavyam.runnr

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private lateinit var searchAdapter: SearchAdapter
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        PlayerManager.init(requireContext())
        val searchBox = view.findViewById<EditText>(R.id.searchBox)
        val recyclerView = view.findViewById<RecyclerView>(R.id.searchResults)

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
                            Toast.makeText(requireContext(), "Network error: Check your connection", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    searchAdapter.updateList(emptyList())
                }
            }
        })

        return view
    }

    private fun onSongClick(song: SongItem) {
        lifecycleScope.launch {
            try {
                val streamUrl = getStreamUrl(song.encrypted_media_url, song.title)
                Log.d("PlayerManager", "Stream URL: $streamUrl")
                if (!streamUrl.isNullOrEmpty()) {
                    PlayerManager.playStream(streamUrl)
                    updatePlayerBarUI(song)
                } else {
                    Toast.makeText(requireContext(), "Failed to load stream", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Playback error. Check connection.", Toast.LENGTH_SHORT).show()
                Log.e("PlayerManager", "Stream failed", e)
            }
        }
    }

    private fun updatePlayerBarUI(song: SongItem) {
        val activity = requireActivity()
        val playerBar = activity.findViewById<View>(R.id.playerBar)
            ?: run {
                Log.e("PlayerBar", "playerBar include not found")
                return
            }

        val cardView = playerBar.findViewById<View>(R.id.playerBarCard)
        playerBar.visibility = View.VISIBLE
        val title = playerBar.findViewById<TextView>(R.id.playerTitle)
        val subtitle = playerBar.findViewById<TextView>(R.id.playerSubtitle)
        val image = playerBar.findViewById<ImageView>(R.id.playerImage)
        val playPause = playerBar.findViewById<ImageView>(R.id.playPauseBtn)
        val colorLayer = playerBar.findViewById<View>(R.id.colorLayer)
        val nightOverlay = playerBar.findViewById<View>(R.id.nightOverlay)

        title?.text = song.title
        subtitle?.text = song.subtitle

        Glide.with(requireContext())
            .asBitmap()
            .load(song.image)
            .into(object : com.bumptech.glide.request.target.BitmapImageViewTarget(image) {
                override fun setResource(resource: android.graphics.Bitmap?) {
                    super.setResource(resource)
                    resource?.let { bitmap ->
                        androidx.palette.graphics.Palette.from(bitmap).generate { palette ->
                            val dominantColor = palette?.getDominantColor(
                                ContextCompat.getColor(requireContext(), R.color.Richblack)
                            ) ?: ContextCompat.getColor(requireContext(), R.color.Richblack)

                            colorLayer?.setBackgroundColor(dominantColor)
                            nightOverlay?.alpha = 0.75f
                        }
                    }
                }
            })

        playPause?.setImageResource(R.drawable.ic_pause)
        playPause?.setOnClickListener {
            val player = PlayerManager.getPlayer()
            if (player?.isPlaying == true) {
                PlayerManager.pause()
                playPause.setImageResource(R.drawable.ic_play)
            } else {
                PlayerManager.resume()
                playPause.setImageResource(R.drawable.ic_pause)
            }
        }
    }
}