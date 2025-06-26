package com.bhavyam.runnr.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bhavyam.runnr.R
import com.bhavyam.runnr.models.SongItem
import com.bumptech.glide.Glide

class SearchAdapter(private val onItemClick: (SongItem) -> Unit) : RecyclerView.Adapter<SearchAdapter.SongViewHolder>() {

    private var songList: List<SongItem> = listOf()

    fun updateList(newList: List<SongItem>) {
        songList = newList
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun getItemCount(): Int = songList.size

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songList[position]
        holder.title.text = song.title
        holder.subtitle.text = song.subtitle
        holder.duration.text = formatDuration(song.duration)
        Glide.with(holder.itemView.context).load(song.image).into(holder.image)

        holder.itemView.setOnClickListener {
            onItemClick(song)
        }
    }

    private fun formatDuration(duration: String): String {
        val totalSecs = duration.toIntOrNull() ?: 0
        val minutes = totalSecs / 60
        val seconds = totalSecs % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.songImage)
        val title: TextView = itemView.findViewById(R.id.songTitle)
        val subtitle: TextView = itemView.findViewById(R.id.songSubtitle)
        val duration: TextView = itemView.findViewById(R.id.songDuration)
    }
}