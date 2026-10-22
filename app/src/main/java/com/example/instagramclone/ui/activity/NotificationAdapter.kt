package com.example.instagramclone.ui.activity

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.instagramclone.R
import com.example.instagramclone.data.model.Notification
import com.example.instagramclone.data.repository.FirestoreRepository

class NotificationAdapter(
    private val notifications: MutableList<Notification>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private val repository = FirestoreRepository()

    class NotificationViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val profileImage: ImageView =
            itemView.findViewById(R.id.ivNotificationProfile)

        val notificationText: TextView =
            itemView.findViewById(R.id.tvNotificationText)

        val notificationTime: TextView =
            itemView.findViewById(R.id.tvNotificationTime)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotificationViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_notification,
                parent,
                false
            )

        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: NotificationViewHolder,
        position: Int
    ) {

        val notification = notifications[position]

        Log.d(
            "NOTIFICATION_ADAPTER",
            "Binding actorId = ${notification.actorId}"
        )

        holder.notificationText.text = "Loading..."
        holder.notificationTime.text =
            getTimeAgo(notification.createdAt)

        repository.getUserById(
            uid = notification.actorId,

            onSuccess = { user ->

                Log.d(
                    "NOTIFICATION_ADAPTER",
                    "User fetched = ${user?.username}"
                )

                val username =
                    user?.username
                        ?.takeIf { it.isNotBlank() }
                        ?: user?.displayName
                            ?.takeIf { it.isNotBlank() }
                        ?: "User"

                holder.notificationText.text =
                    when (notification.type) {

                        "follow" ->
                            "$username started following you"

                        "like" ->
                            "$username liked your post"

                        "comment" ->
                            "$username commented on your post"

                        else ->
                            username
                    }

                Log.d(
                    "NOTIFICATION_ADAPTER",
                    "Showing username = $username"
                )

                val profileImage =
                    user?.profileImageUrl.orEmpty()

                if (profileImage.isNotBlank()) {
                    loadProfileImage(
                        holder.profileImage,
                        profileImage
                    )
                }
            },

            onFailure = { exception ->

                Log.e(
                    "NOTIFICATION_ADAPTER",
                    "Failed user fetch = ${exception.message}"
                )

                holder.notificationText.text =
                    "${notification.actorId} started following you"
            }
        )
    }

    override fun getItemCount(): Int {
        return notifications.size
    }

    fun updateNotifications(
        newNotifications: List<Notification>
    ) {

        Log.d(
            "NOTIFICATION_ADAPTER",
            "Notifications received = ${newNotifications.size}"
        )

        notifications.clear()
        notifications.addAll(newNotifications)

        notifyDataSetChanged()
    }

    private fun loadProfileImage(
        imageView: ImageView,
        base64Image: String
    ) {
        try {

            val imageBytes =
                Base64.decode(
                    base64Image,
                    Base64.DEFAULT
                )

            val bitmap =
                BitmapFactory.decodeByteArray(
                    imageBytes,
                    0,
                    imageBytes.size
                )

            imageView.setImageBitmap(bitmap)

        } catch (e: Exception) {

            Log.e(
                "NOTIFICATION_ADAPTER",
                "Image error = ${e.message}"
            )
        }
    }

    private fun getTimeAgo(
        timestamp: Long
    ): String {

        val diff =
            System.currentTimeMillis() - timestamp

        val minutes =
            diff / (1000 * 60)

        val hours =
            minutes / 60

        val days =
            hours / 24

        return when {

            minutes < 1 ->
                "Just now"

            minutes < 60 ->
                "${minutes}m"

            hours < 24 ->
                "${hours}h"

            else ->
                "${days}d"
        }
    }
}