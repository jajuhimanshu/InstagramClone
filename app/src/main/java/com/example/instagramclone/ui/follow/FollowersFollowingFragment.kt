package com.example.instagramclone.ui.follow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.instagramclone.R
import com.example.instagramclone.data.model.User
import com.example.instagramclone.ui.profile.ProfileFragment
import com.google.firebase.firestore.FirebaseFirestore

class FollowersFollowingFragment : Fragment() {

    private lateinit var rvUsers: RecyclerView
    private lateinit var adapter: FollowUserAdapter
    private val userList = mutableListOf<User>()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(
            R.layout.fragment_followers_following,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        rvUsers = view.findViewById(R.id.rvUsers)
        adapter = FollowUserAdapter(userList) { clickedUser ->

            val profileFragment = ProfileFragment()
            val bundle = Bundle()
            bundle.putString("userId", clickedUser.uid)

            profileFragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, profileFragment)
                .addToBackStack(null)
                .commit()
        }

        rvUsers.layoutManager = LinearLayoutManager(requireContext())
        rvUsers.adapter = adapter

        val userId = arguments?.getString("userId")
        val type = arguments?.getString("type")
        Log.d("FOLLOW_DEBUG", "userId = $userId , type = $type")
        if (userId != null && type != null) {
            if (type == "followers") {
                loadFollowers(userId)
            } else {
                loadFollowing(userId)
            }
        }
    }

    private fun loadFollowers(userId: String) {
        firestore.collection("followers")
            .document(userId)
            .collection("userFollowers")
            .get()
            .addOnSuccessListener { snapshots ->
                Log.d("FOLLOWERS_DEBUG", "Documents Found = ${snapshots.size()}")
                userList.clear()
                for (document in snapshots.documents) {
                    Log.d("FOLLOWERS_DEBUG", "Follower UID = ${document.id}")
                    val followerUid = document.id
                    firestore.collection("users")
                        .document(followerUid)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            Log.d("FOLLOWERS_DEBUG", "User Found = ${userDoc.id}")
                            val user = userDoc.toObject(User::class.java)
                            if (user != null) {
                                val userWithId = User(
                                    uid = userDoc.id,
                                    username = user.username,
                                    usernameLowercase = user.usernameLowercase,
                                    displayName = user.displayName,
                                    bio = user.bio,
                                    profileImageUrl = user.profileImageUrl,
                                    createdAt = user.createdAt
                                )
                                userList.add(userWithId)
                                adapter.notifyDataSetChanged()
                            }
                        }
                }
            }
    }

    private fun loadFollowing(userId: String) {
        firestore.collection("following")
            .document(userId)
            .collection("userFollowing")
            .get()
            .addOnSuccessListener { snapshots ->
                Log.d("FOLLOW_DEBUG", "Documents found = ${snapshots.size()}")
                userList.clear()
                for (document in snapshots.documents) {
                    val followingUid = document.id
                    firestore.collection("users")
                        .document(followingUid)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val user = userDoc.toObject(User::class.java)

                            if (user != null) {
                                val userWithId = User(
                                    uid = userDoc.id,
                                    username = user.username,
                                    usernameLowercase = user.usernameLowercase,
                                    displayName = user.displayName,
                                    bio = user.bio,
                                    profileImageUrl = user.profileImageUrl,
                                    createdAt = user.createdAt
                                )
                                userList.add(userWithId)
                                adapter.notifyDataSetChanged()
                            }
                        }
                }
            }
    }
}