package com.example.instagramclone.data.repository

import android.util.Log
import com.example.instagramclone.data.model.Notification
import com.google.firebase.firestore.FirebaseFirestore

class NotificationRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun createFollowNotification(
        actorId: String,
        recipientId: String
    ) {
        if (actorId == recipientId) {
            return
        }
        Log.d(
            "NOTIFICATION_DEBUG",
            "Creating notification: $actorId -> $recipientId"
        )

        val notificationId =
            firestore.collection("notifications")
                .document()
                .id

        val notification = Notification(
            notificationId = notificationId,
            recipientId = recipientId,
            actorId = actorId,
            type = "follow",
            postId = "",
            commentId = "",
            createdAt = System.currentTimeMillis(),
            isRead = false
        )

        firestore.collection("notifications")
            .document(notificationId)
            .set(notification)
            .addOnSuccessListener {

                Log.d(
                    "NOTIFICATION_DEBUG",
                    "Notification saved successfully"
                )

            }
            .addOnFailureListener { e ->
                Log.e(
                    "NOTIFICATION_DEBUG",
                    "Failed: ${e.message}"
                )

            }
    }

    fun getNotifications(
        userId: String,
        onSuccess: (List<Notification>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d(
            "ACTIVITY_DEBUG",
            "Searching notifications for: $userId"
        )

        firestore.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d(
                    "ACTIVITY_DEBUG",
                    "Documents found = ${snapshot.size()}"
                )

                val notifications =
                    snapshot.documents.mapNotNull {
                        it.toObject(Notification::class.java)
                    }.sortedByDescending {
                        it.createdAt
                    }

                Log.d(
                    "ACTIVITY_DEBUG",
                    "Mapped notifications = ${notifications.size}"
                )

                onSuccess(notifications)
            }
            .addOnFailureListener { e ->
                Log.e(
                    "ACTIVITY_DEBUG",
                    "Error = ${e.message}"
                )
                onFailure(e)
            }
    }


    fun createNotification(
        actorId: String,
        recipientId: String,
        type: String,
        postId: String = "",
        commentId: String = ""
    ) {

        if (actorId == recipientId) {
            return
        }

        val notificationId =
            firestore.collection("notifications")
                .document()
                .id

        val notification = Notification(
            notificationId = notificationId,
            recipientId = recipientId,
            actorId = actorId,
            type = type,
            postId = postId,
            commentId = commentId,
            createdAt = System.currentTimeMillis(),
            isRead = false
        )

        firestore.collection("notifications")
            .document(notificationId)
            .set(notification)
    }

}