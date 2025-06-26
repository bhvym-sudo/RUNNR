package com.bhavyam.runnr

import com.bhavyam.runnr.models.SongItem

object PlaybackQueueManager {
    private var songList: List<SongItem> = emptyList()
    private var currentIndex: Int = -1

    fun setQueue(songs: List<SongItem>, startIndex: Int) {
        songList = songs
        currentIndex = startIndex
    }

    fun getCurrentSong(): SongItem? {
        return if (currentIndex in songList.indices) songList[currentIndex] else null
    }

    fun getNextSong(): SongItem? {
        return if (currentIndex + 1 < songList.size) songList[++currentIndex] else null
    }

    fun getPreviousSong(): SongItem? {
        return if (currentIndex - 1 >= 0) songList[--currentIndex] else null
    }
}
