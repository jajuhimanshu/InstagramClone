package com.example.instagramclone

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ProfilePostAdapter(
    private val imageList: MutableList<String>
) : RecyclerView.Adapter<ProfilePostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val ivPost: ImageView =
            view.findViewById(R.id.ivPost)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PostViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_profile_post,
                parent,
                false
            )

        return PostViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: PostViewHolder,
        position: Int
    ) {

        try {

            val imageBytes = Base64.decode(
                imageList[position],
                Base64.DEFAULT
            )

            val bitmap = BitmapFactory.decodeByteArray(
                imageBytes,
                0,
                imageBytes.size
            )

            holder.ivPost.setImageBitmap(bitmap)

        } catch (_: Exception) {

        }
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    fun updateImages(
        images: List<String>
    ) {
        imageList.clear()
        imageList.addAll(images)
        notifyDataSetChanged()
    }
}