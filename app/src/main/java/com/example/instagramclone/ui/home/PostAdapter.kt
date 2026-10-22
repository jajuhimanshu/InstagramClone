package com.example.instagramclone.ui.home

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.instagramclone.R
import com.example.instagramclone.data.model.Post
import com.example.instagramclone.data.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.Executors

class PostAdapter(
    private val posts: MutableList<Post>,
    private val onLikeClicked: (Post) -> Unit,
    private val onCommentClicked: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val database = FirebaseDatabase.getInstance(
        "https://instagramclone-3fbc0-default-rtdb.firebaseio.com"
    ).reference

    private val firestoreRepository = FirestoreRepository()
    private val auth = FirebaseAuth.getInstance()
    private val imageExecutor = Executors.newFixedThreadPool(2)

    private val usernameCache = mutableMapOf<String, String>()
    private val profileImageCache = mutableMapOf<String, String>()

    private val likedPosts = mutableMapOf<String, Boolean>()
    private val loadingLikes = mutableSetOf<String>()
    private val processingLikes = mutableSetOf<String>()

    class PostViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val avatar: ImageView = itemView.findViewById(R.id.ivPostAvatar)
        val username: TextView = itemView.findViewById(R.id.tvPostUsername)
        val subtitle: TextView = itemView.findViewById(R.id.tvPostSubtitle)
        val postImage: ImageView = itemView.findViewById(R.id.ivPostImage)
        val likeButton: ImageView = itemView.findViewById(R.id.btnLike)
        val commentButton: ImageView = itemView.findViewById(R.id.btnComment)
        val shareButton: ImageView = itemView.findViewById(R.id.btnShare)
        val bookmarkButton: ImageView = itemView.findViewById(R.id.btnBookmark)
        val likeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        val commentCount: TextView = itemView.findViewById(R.id.tvCommentCount)
        val caption: TextView = itemView.findViewById(R.id.tvPostCaption)
        val viewAllComments: TextView =
            itemView.findViewById(R.id.tvViewAllComments)
        val timestamp: TextView = itemView.findViewById(R.id.tvPostTimestamp)
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
        val postId = post.postId


        holder.username.text = "Loading..."
        holder.avatar.setImageDrawable(null)
        holder.avatar.tag = postId

        holder.subtitle.text = ""
        holder.caption.text = post.caption
        holder.likeCount.text = post.likeCount.toString()
        holder.commentCount.text = post.commentCount.toString()

        holder.viewAllComments.visibility =
            if (post.commentCount > 0L) View.VISIBLE else View.GONE

        holder.viewAllComments.text = when (post.commentCount) {
            1L -> "View all 1 comment"
            else -> "View all ${post.commentCount} comments"
        }

        holder.timestamp.text = ""
        holder.postImage.setImageDrawable(null)
        holder.postImage.tag = postId

        updateLikeButton(
            holder.likeButton,
            likedPosts[postId] == true
        )


        val cachedUsername = usernameCache[post.authorId]

        if (cachedUsername != null) {
            holder.username.text = cachedUsername
        } else {
            holder.username.text = post.authorId
        }

        val cachedProfileImage = profileImageCache[post.authorId]

        if (cachedProfileImage != null) {
            displayProfileImage(
                holder.avatar,
                cachedProfileImage,
                postId
            )
        }

        if (cachedUsername == null || cachedProfileImage == null) {
            firestoreRepository.getUserById(
                uid = post.authorId,
                onSuccess = { user ->
                    val loadedUsername = user?.username
                        ?.takeIf { it.isNotBlank() }
                        ?: user?.displayName?.takeIf { it.isNotBlank() }
                        ?: post.authorId

                    usernameCache[post.authorId] = loadedUsername

                    val profileImage = user?.profileImageUrl.orEmpty()

                    if (profileImage.isNotBlank()) {
                        profileImageCache[post.authorId] = profileImage
                    }

                    if (holder.postImage.tag == postId) {
                        holder.username.text = loadedUsername
                    }

                    if (profileImage.isNotBlank()) {
                        displayProfileImage(
                            holder.avatar,
                            profileImage,
                            postId
                        )
                    }
                },
                onFailure = {}
            )
        }


        val currentUid = auth.currentUser?.uid

        if (currentUid != null && loadingLikes.add(postId)) {
            firestoreRepository.checkIfPostLiked(
                postId = postId,
                userId = currentUid,
                onSuccess = { isLiked ->
                    likedPosts[postId] = isLiked
                    loadingLikes.remove(postId)

                    if (holder.postImage.tag == postId) {
                        updateLikeButton(holder.likeButton, isLiked)
                    }
                },
                onFailure = {
                    loadingLikes.remove(postId)
                }
            )
        }


        imageExecutor.execute {
            database.child("postImages")
                .child(post.authorId)
                .child(postId)
                .get()
                .addOnSuccessListener { snapshot ->
                    val base64Image = snapshot.getValue(String::class.java)

                    if (base64Image.isNullOrBlank()) {
                        return@addOnSuccessListener
                    }

                    try {
                        val imageBytes = Base64.decode(
                            base64Image,
                            Base64.DEFAULT
                        )

                        val bitmap = BitmapFactory.decodeByteArray(
                            imageBytes,
                            0,
                            imageBytes.size
                        )

                        if (bitmap != null) {
                            holder.postImage.post {
                                if (holder.postImage.tag == postId) {
                                    holder.postImage.setImageBitmap(bitmap)
                                }
                            }
                        }
                    } catch (_: IllegalArgumentException) {

                    }
                }
                .addOnFailureListener {

                }
        }


        holder.likeButton.setOnClickListener {
            val userId = auth.currentUser?.uid

            if (userId == null) {
                Toast.makeText(
                    holder.itemView.context,
                    "Please log in to like posts",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (!processingLikes.add(postId)) {
                return@setOnClickListener
            }

            holder.likeButton.isEnabled = false

            firestoreRepository.togglePostLike(
                postId = postId,
                userId = userId,
                postOwnerId = post.authorId,
                onSuccess = { isLiked, newCount ->
                    likedPosts[postId] = isLiked

                    val postIndex = posts.indexOfFirst {
                        it.postId == postId
                    }

                    if (postIndex != -1) {
                        posts[postIndex] = posts[postIndex].copy(
                            likeCount = newCount
                        )
                        notifyItemChanged(postIndex)
                    }

                    processingLikes.remove(postId)
                    holder.likeButton.isEnabled = true
                    updateLikeButton(holder.likeButton, isLiked)
                },
                onFailure = { exception ->
                    processingLikes.remove(postId)
                    holder.likeButton.isEnabled = true

                    Toast.makeText(
                        holder.itemView.context,
                        "Couldn't update like: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }

        // Open comments.
        holder.commentButton.setOnClickListener {
            onCommentClicked(post)
        }

        holder.commentCount.setOnClickListener {
            onCommentClicked(post)
        }

        holder.viewAllComments.setOnClickListener {
            onCommentClicked(post)
        }


        holder.shareButton.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, post.caption)
            }

            holder.itemView.context.startActivity(
                Intent.createChooser(shareIntent, "Share post")
            )
        }


        holder.bookmarkButton.setOnClickListener {
            Toast.makeText(
                holder.itemView.context,
                "Saving posts isn't available yet",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun displayProfileImage(
        imageView: ImageView,
        base64Image: String,
        postId: String
    ) {
        imageExecutor.execute {
            try {
                val imageBytes = Base64.decode(
                    base64Image,
                    Base64.DEFAULT
                )

                val bitmap: Bitmap? = BitmapFactory.decodeByteArray(
                    imageBytes,
                    0,
                    imageBytes.size
                )

                if (bitmap != null) {
                    imageView.post {
                        if (imageView.tag == postId) {
                            imageView.setImageBitmap(bitmap)
                        }
                    }
                }
            } catch (_: IllegalArgumentException) {

            }
        }
    }

    private fun updateLikeButton(
        button: ImageView,
        isLiked: Boolean
    ) {
        button.setImageResource(
            if (isLiked) {
                R.drawable.ic_heart_filled
            } else {
                R.drawable.ic_heart_outline
            }
        )

        button.contentDescription =
            if (isLiked) "Unlike post" else "Like post"
    }

    override fun getItemCount(): Int = posts.size

    fun updatePosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }
}
