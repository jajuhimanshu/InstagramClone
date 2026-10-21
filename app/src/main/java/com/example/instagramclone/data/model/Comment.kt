package com.example.instagramclone.data.model

data class Comment(
    val commentId: String = "",
    val authorId: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)
