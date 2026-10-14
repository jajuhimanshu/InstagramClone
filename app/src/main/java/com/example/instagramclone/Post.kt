
package com.example.instagramclone

data class Post(
    val postId: String = "",
    val authorId: String = "",
    val imageUrl: String = "",
    val caption: String = "",
    val createdAt: Long = 0L,
    val likeCount: Long = 0L,
    val commentCount: Long = 0L
)
