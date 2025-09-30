package com.bhavyam.runnr

import com.bhavyam.runnr.models.SongItem

object PlaybackQueueManager {
    private var songList: List<SongItem> = emptyList()
    private var currentIndex: Int = -1

    fun setQueue(songs: List<SongItem>, startIndex: Int) {
        songList = songs
        currentIndex = startIndex
    }

    fun setCurrentIndex(index: Int) {
        currentIndex = index
    }

    fun getCurrentIndex(): Int {
        return currentIndex
    }

    fun getCurrentSong(): SongItem? {
        return if (currentIndex in songList.indices) songList[currentIndex] else null
    }

    fun getNextSong(): SongItem? {
        return if (currentIndex + 1 < songList.size) {
            currentIndex++
            songList[currentIndex]
        } else null
    }

    fun getPreviousSong(): SongItem? {
        return if (currentIndex - 1 >= 0) {
            currentIndex--
            songList[currentIndex]
        } else null
    }

    fun hasNextSong(): Boolean {
        return currentIndex + 1 < songList.size
    }

    fun hasPreviousSong(): Boolean {
        return currentIndex - 1 >= 0
    }

    fun getQueueSize(): Int {
        return songList.size
    }

    fun clearQueue() {
        songList = emptyList()
        currentIndex = -1
    }
}
