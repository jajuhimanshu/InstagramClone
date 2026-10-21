package com.example.instagramclone.util

import android.graphics.BitmapFactory
import android.widget.ImageView
import android.util.Base64
import com.example.instagramclone.R

fun ImageView.loadProfileImage(imageData: String?) {

    if (!imageData.isNullOrEmpty()) {
        try {
            val bytes = Base64.decode(imageData, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            this.setImageBitmap(bitmap)
        } catch (e: Exception) {
            this.setImageResource(R.mipmap.ic_launcher_round)
        }
    } else {
        this.setImageResource(R.mipmap.ic_launcher_round)
    }
}
