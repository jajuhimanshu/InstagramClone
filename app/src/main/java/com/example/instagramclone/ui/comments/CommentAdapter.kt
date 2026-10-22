package com.example.instagramclone.ui.comments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.instagramclone.R
import com.example.instagramclone.data.model.Comment
import com.example.instagramclone.data.repository.FirestoreRepository

class CommentAdapter(
    private val comments: MutableList<Comment>
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val repository = FirestoreRepository()
    private val usernameCache = mutableMapOf<String, String>()

    class CommentViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val username: TextView = itemView.findViewById(R.id.tvCommentUsername)
        val commentText: TextView = itemView.findViewById(R.id.tvCommentText)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)

        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CommentViewHolder,
        position: Int
    ) {
        val comment = comments[position]

        holder.commentText.text = comment.text
        holder.username.text = "User"

        val cachedUsername = usernameCache[comment.authorId]

        if (cachedUsername != null) {
            holder.username.text = cachedUsername
        } else {
            repository.getUserById(
                uid = comment.authorId,
                onSuccess = { user ->
                    val username = user?.username
                        ?.takeIf { it.isNotBlank() }
                        ?: "User"

                    usernameCache[comment.authorId] = username

                    val currentPosition = holder.bindingAdapterPosition

                    if (currentPosition != RecyclerView.NO_POSITION &&
                        comments[currentPosition].commentId == comment.commentId
                    ) {
                        holder.username.text = username
                    }
                },
                onFailure = {

                }
            )
        }
    }

    override fun getItemCount(): Int = comments.size

    fun updateComments(newComments: List<Comment>) {
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
    }
}
