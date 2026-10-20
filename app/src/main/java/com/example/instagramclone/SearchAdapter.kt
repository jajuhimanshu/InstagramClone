package com.example.instagramclone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class SearchAdapter(private var userList: MutableList<User>,private val onItemClicked:(User) -> Unit) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    class SearchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: CircleImageView = view.findViewById(R.id.ivSearchProfileImage)
        val username: TextView = view.findViewById(R.id.tvSearchUsername)
        val displayName: TextView = view.findViewById(R.id.tvSearchDisplayName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_search, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val user = userList[position]
        holder.username.text = user.username
        holder.displayName.text = user.displayName

        holder.profileImage.loadProfileImage(user.profileImageUrl)
        holder.itemView.setOnClickListener { onItemClicked(user) }
    }

    override fun getItemCount(): Int = userList.size

    fun updateList(newList: List<User>) {
        userList.clear()
        userList.addAll(newList)
        notifyDataSetChanged()
    }
}
