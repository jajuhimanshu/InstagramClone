package com.example.instagramclone

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var tvUsername: TextView
    private lateinit var ivMenu: ImageView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvPostCount: TextView
    private lateinit var tvFollowersCount: TextView
    private lateinit var tvFollowingCount: TextView
    private lateinit var layoutFollowers: LinearLayout
    private lateinit var layoutFollowing: LinearLayout


    private lateinit var ivProfileImage : ImageView
    private lateinit var btnEditProfile: Button
    private lateinit var btnShareProfile: Button

    private lateinit var rvProfilePosts: RecyclerView

    private lateinit var profilePostAdapter: ProfilePostAdapter

    private lateinit var layoutEmptyPosts: LinearLayout

    private val imageList = mutableListOf<String>()


    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnShareProfile = view.findViewById(R.id.btnShareProfile)

        tvUsername = view.findViewById(R.id.tvUsername)
        tvDisplayName = view.findViewById(R.id.tvDisplayName)
        tvBio = view.findViewById(R.id.tvBio)
        ivProfileImage = view.findViewById(R.id.ivProfileImage)
        tvPostCount = view.findViewById(R.id.tvPostCount)
        tvFollowersCount = view.findViewById(R.id.tvFollowersCount)
        tvFollowingCount = view.findViewById(R.id.tvFollowingCount)
        layoutFollowers = view.findViewById(R.id.layoutFollowers)
        layoutFollowing = view.findViewById(R.id.layoutFollowing)
        layoutEmptyPosts = view.findViewById(R.id.layoutEmptyPosts)

        ivMenu = view.findViewById(R.id.ivMenu)

        btnEditProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, EditProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        btnShareProfile.setOnClickListener {
            shareProfile()
        }

        ivMenu.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        layoutFollowers.setOnClickListener {
            val profileUserId = arguments?.getString("userId")
                    ?: auth.currentUser?.uid
            val fragment = FollowersFollowingFragment()
            val bundle = Bundle()
            bundle.putString("userId", profileUserId)
            bundle.putString("type", "followers")
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        layoutFollowing.setOnClickListener {

            val fragment = FollowersFollowingFragment()
            val bundle = Bundle()
            bundle.putString("userId", auth.currentUser?.uid)
            bundle.putString("type", "following")
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        rvProfilePosts = view.findViewById(R.id.rvProfilePosts)

        profilePostAdapter = ProfilePostAdapter(imageList)
        rvProfilePosts.layoutManager = GridLayoutManager(requireContext(), 3)
        rvProfilePosts.adapter = profilePostAdapter

        return view
    }


    override fun onResume() {
        super.onResume()

        val currentUid = auth.currentUser?.uid
        val passedUserId = arguments?.getString("userId")

        if (passedUserId != null) {
            loadUserDataFromFirestore(passedUserId)
            loadUserPosts(passedUserId)
            checkIfFollowing(passedUserId)
            btnEditProfile.setOnClickListener {
                if (btnEditProfile.text == "Follow") {
                    followUser(passedUserId)
                } else {
                    unfollowUser(passedUserId)
                }
            }

        } else {
            btnEditProfile.text = "Edit Profile"
            btnEditProfile.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, EditProfileFragment())
                    .addToBackStack(null)
                    .commit()
            }

            if (currentUid != null) {
                loadUserDataFromFirestore(currentUid)
                loadUserPosts(currentUid)
            }
        }
    }

    private fun loadUserDataFromFirestore(userId: String) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val user = documentSnapshot.toObject(User::class.java)

                    if (user != null) {
                        android.util.Log.d(
                            "PROFILE_IMAGE",
                            "Length = ${user.profileImageUrl.length}"
                        )
                        tvUsername.text = user.username
                        tvDisplayName.text = user.displayName
                        tvBio.text = user.bio
                        if (user.profileImageUrl.isNotEmpty()) {
                            ivProfileImage.setImageBitmap(
                                base64ToBitmap(user.profileImageUrl))
                        }
                        loadFollowersCount(userId)
                        loadFollowingCount(userId)
                    }
                }
            }
            .addOnFailureListener { exception ->
                android.widget.Toast.makeText(
                    requireContext(),
                    "Firebase Says: ${exception.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                exception.printStackTrace()
            }

    }

    private fun base64ToBitmap(
        base64String: String
    ): Bitmap {
        val decodedBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size
        )
    }

    private fun shareProfile() {

        val username = tvUsername.text.toString()
        val profileLink = "instagramclone://profile/$username"

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out my profile:\n$profileLink")
        startActivity(Intent.createChooser(shareIntent, "Share Profile"))
    }

    private fun followUser(targetUserId: String) {
        Toast.makeText(requireContext(), "Follow Clicked", Toast.LENGTH_SHORT).show()
        val currentUid = FirebaseAuth.getInstance()
                .currentUser?.uid ?: return

        android.util.Log.d("FOLLOW_DEBUG", "currentUid = $currentUid")
        android.util.Log.d("FOLLOW_DEBUG", "targetUserId = $targetUserId")

        firestore.collection("followers")
            .document(targetUserId)
            .collection("userFollowers")
            .document(currentUid)
            .set(hashMapOf("uid" to currentUid, "followedAt" to System.currentTimeMillis()))
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Followers Updated", Toast.LENGTH_SHORT).show()
                loadFollowersCount(targetUserId)
                loadFollowingCount(currentUid)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                android.util.Log.e("FOLLOW_ERROR", e.message ?: "Unknown Error")
            }

        firestore.collection("following")
            .document(currentUid)
            .collection("userFollowing")
            .document(targetUserId)
            .set(hashMapOf("uid" to targetUserId, "followedAt" to System.currentTimeMillis()))
            .addOnSuccessListener {
                android.widget.Toast.makeText(requireContext(), "Followed Successfully", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkIfFollowing(targetUserId: String) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        firestore.collection("following")
            .document(currentUid)
            .collection("userFollowing")
            .document(targetUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    btnEditProfile.text = "Following"
                } else {
                    btnEditProfile.text = "Follow"
                }
            }
    }

    private fun unfollowUser(targetUserId: String) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        firestore.collection("followers")
            .document(targetUserId)
            .collection("userFollowers")
            .document(currentUid)
            .delete()

        firestore.collection("following")
            .document(currentUid)
            .collection("userFollowing")
            .document(targetUserId)
            .delete()
            .addOnSuccessListener {
                btnEditProfile.text = "Follow"
                Toast.makeText(requireContext(), "Unfollowed", Toast.LENGTH_SHORT).show()
                loadFollowersCount(targetUserId)
                loadFollowingCount(currentUid)
            }
    }

    private fun loadFollowersCount(userId: String) {
        firestore.collection("followers")
            .document(userId)
            .collection("userFollowers")
            .get()
            .addOnSuccessListener { snapshot ->
                tvFollowersCount.text = snapshot.size().toString()
            }
    }

    private fun loadFollowingCount(userId: String) {
        firestore.collection("following")
            .document(userId)
            .collection("userFollowing")
            .get()
            .addOnSuccessListener { snapshot ->
                tvFollowingCount.text = snapshot.size().toString()
            }
    }

    private fun loadUserPosts(
        userId: String
    ) {
        FirebaseFirestore.getInstance()
            .collection("posts")
            .whereEqualTo("authorId", userId)
            .get()
            .addOnSuccessListener { snapshots ->
                tvPostCount.text = snapshots.size().toString()
                val images = mutableListOf<String>()
                for (document in snapshots.documents) {
                    val post = document.toObject(Post::class.java)

                    if (post != null) {
                        FirebaseDatabase
                            .getInstance("https://instagramclone-3fbc0-default-rtdb.firebaseio.com")
                            .reference
                            .child("postImages")
                            .child(userId)
                            .child(post.postId)
                            .get()
                            .addOnSuccessListener {
                                val base64 = it.getValue(String::class.java)
                                if (!base64.isNullOrEmpty()) {
                                    images.add(base64)
                                    profilePostAdapter.updateImages(images)
                                }
                            }
                    }
                    if (snapshots.isEmpty) {
                        layoutEmptyPosts.visibility = View.VISIBLE
                        rvProfilePosts.visibility = View.GONE

                    } else {
                        layoutEmptyPosts.visibility = View.GONE
                        rvProfilePosts.visibility = View.VISIBLE
                    }
                }
            }
    }
}