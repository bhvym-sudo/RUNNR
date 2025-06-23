package com.bhavyam.runnr.utils

import android.content.Context
import android.widget.ImageView
import com.bhavyam.runnr.R
import com.bhavyam.runnr.models.SongItem
import com.bhavyam.runnr.storage.LikedSongsManager

fun updateLikeIcon(context: Context, button: ImageView, song: SongItem?) {
    if (song != null && LikedSongsManager.isLiked(context, song)) {
        button.setImageResource(R.drawable.ic_heart_filled)
    } else {
        button.setImageResource(R.drawable.ic_heart_outline)
    }
}
