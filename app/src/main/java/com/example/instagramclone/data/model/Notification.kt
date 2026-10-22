package com.example.instagramclone.data.model

data class Notification(
    val notificationId: String = "",
    val recipientId: String = "",
    val actorId: String = "",
    val type: String = "",
    val postId: String = "",
    val commentId: String = "",
    val createdAt: Long = 0L,
    val isRead: Boolean = false
)
