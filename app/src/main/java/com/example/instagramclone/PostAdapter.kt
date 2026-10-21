
package com.example.instagramclone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PostAdapter(
    private val posts: MutableList<Post>,
    private val onLikeClicked: (Post) -> Unit,
    private val onCommentClicked: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val avatar: TextView =
            itemView.findViewById(R.id.tvPostAvatar)

        val username: TextView =
            itemView.findViewById(R.id.tvPostUsername)

        val subtitle: TextView =
            itemView.findViewById(R.id.tvPostSubtitle)

        val postImage: ImageView =
            itemView.findViewById(R.id.ivPostImage)

        val likeButton: TextView =
            itemView.findViewById(R.id.btnLike)

        val commentButton: TextView =
            itemView.findViewById(R.id.btnComment)

        val likeCount: TextView =
            itemView.findViewById(R.id.tvLikeCount)

        val caption: TextView =
            itemView.findViewById(R.id.tvPostCaption)

        val commentCount: TextView =
            itemView.findViewById(R.id.tvCommentCount)

        val timestamp: TextView =
            itemView.findViewById(R.id.tvPostTimestamp)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PostViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)

        return PostViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: PostViewHolder,
        position: Int
    ) {
        val post = posts[position]

        holder.username.text = post.authorId
        holder.avatar.text =
            post.authorId.firstOrNull()?.uppercase() ?: "?"

        holder.subtitle.text = ""
        holder.caption.text = post.caption
        holder.likeCount.text = "${post.likeCount} likes"
        holder.commentCount.text =
            "View all ${post.commentCount} comments"

        holder.timestamp.text = ""

        holder.postImage.setImageDrawable(null)
        holder.postImage.contentDescription = "Post image"

        holder.likeButton.setOnClickListener {
            onLikeClicked(post)
        }

        holder.commentButton.setOnClickListener {
            onCommentClicked(post)
        }
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    fun updatePosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }
}
