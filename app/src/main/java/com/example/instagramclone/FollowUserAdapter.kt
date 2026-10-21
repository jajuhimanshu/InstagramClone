package com.example.instagramclone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

class FollowUserAdapter(
    private val userList: MutableList<User>,
    private val onItemClicked: (User) -> Unit
) : RecyclerView.Adapter<FollowUserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {
        val ivProfileImage: CircleImageView = view.findViewById(R.id.ivProfileImage)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvDisplayName: TextView = view.findViewById(R.id.tvDisplayName)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UserViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follow_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {

        val user = userList[position]

        holder.tvUsername.text = user.username
        holder.tvDisplayName.text = user.displayName
        holder.ivProfileImage.loadProfileImage(user.profileImageUrl)
        holder.itemView.setOnClickListener {
            onItemClicked(user)
        }
    }

    override fun getItemCount(): Int = userList.size
}