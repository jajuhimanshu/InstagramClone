
package com.example.instagramclone

data class User(
    val uid: String = "",
    val username: String = "",
    val usernameLowercase: String = "",
    val displayName: String = "",
    val bio: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = 0L
)
