package com.example.instagramclone.ui.home

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.instagramclone.ui.comments.CommentsFragment
import com.example.instagramclone.R
import com.example.instagramclone.data.model.Post
import com.example.instagramclone.data.repository.FirestoreRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var progressHome: ProgressBar
    private lateinit var emptyMessage: TextView
    private lateinit var myStoryImage: ImageView

    private val posts = mutableListOf<Post>()
    private val firestoreRepository = FirestoreRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_home,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)


        val homeRoot = view.findViewById<View>(R.id.homeRoot)

        ViewCompat.setOnApplyWindowInsetsListener(homeRoot) { root, insets ->

            val statusBarInsets = insets.getInsets(
                WindowInsetsCompat.Type.statusBars()
            )

            root.setPadding(
                root.paddingLeft,
                statusBarInsets.top,
                root.paddingRight,
                root.paddingBottom
            )

            insets
        }

        ViewCompat.requestApplyInsets(homeRoot)


        postsRecyclerView = view.findViewById(R.id.rvPosts)
        progressHome = view.findViewById(R.id.progressHome)
        emptyMessage = view.findViewById(R.id.tvHomeEmpty)
        myStoryImage = view.findViewById(R.id.ivMyStory)


        postAdapter = PostAdapter(
            posts = posts,
            onLikeClicked = {

            },
            onCommentClicked = { post ->
                openComments(post)
            }
        )

        postsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        postsRecyclerView.adapter = postAdapter


        view.findViewById<TextView>(R.id.btnHomeCreate)
            .setOnClickListener {
                selectBottomNavigationItem(R.id.nav_create)
            }


        view.findViewById<TextView>(R.id.btnHomeActivity)
            .setOnClickListener {
                selectBottomNavigationItem(R.id.nav_activity)
            }


        loadMyProfileImage()
        loadPosts()
    }

    private fun selectBottomNavigationItem(itemId: Int) {
        val bottomNavigation = requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNavigation)

        bottomNavigation.selectedItemId = itemId
    }

    private fun loadMyProfileImage() {
        val currentUid = FirebaseAuth.getInstance()
            .currentUser?.uid ?: return

        firestoreRepository.getUserById(
            uid = currentUid,
            onSuccess = { user ->

                if (!isAdded || view == null) {
                    return@getUserById
                }

                val base64Image = user?.profileImageUrl

                if (base64Image.isNullOrBlank()) {
                    return@getUserById
                }

                try {
                    val imageBytes = Base64.decode(
                        base64Image,
                        Base64.DEFAULT
                    )

                    val bitmap = BitmapFactory.decodeByteArray(
                        imageBytes,
                        0,
                        imageBytes.size
                    )

                    if (bitmap != null && isAdded) {
                        myStoryImage.setImageBitmap(bitmap)
                    }

                } catch (_: IllegalArgumentException) {

                }
            },
            onFailure = {

            }
        )
    }
    private fun openComments(post: Post) {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainer,
                CommentsFragment.newInstance(post.postId,post.authorId)
            )
            .addToBackStack(null)
            .commit()
    }

    private fun loadPosts() {
        val currentUid = FirebaseAuth.getInstance()
            .currentUser?.uid

        if (currentUid == null) {
            progressHome.visibility = View.GONE
            emptyMessage.text = "Please log in to view posts"
            emptyMessage.visibility = View.VISIBLE
            return
        }

        progressHome.visibility = View.VISIBLE
        emptyMessage.visibility = View.GONE

        firestoreRepository.getHomeFeedPosts(
            currentUid = currentUid,

            onSuccess = { loadedPosts ->

                if (!isAdded || view == null) {
                    return@getHomeFeedPosts
                }

                progressHome.visibility = View.GONE

                postAdapter.updatePosts(loadedPosts)

                if (loadedPosts.isEmpty()) {
                    emptyMessage.text = "No posts yet"
                    emptyMessage.visibility = View.VISIBLE
                } else {
                    emptyMessage.visibility = View.GONE
                }
            },

            onFailure = { exception ->

                if (!isAdded || view == null) {
                    return@getHomeFeedPosts
                }

                progressHome.visibility = View.GONE

                emptyMessage.text =
                    "Couldn't load posts. Please try again."

                emptyMessage.visibility = View.VISIBLE

                Toast.makeText(
                    requireContext(),
                    "Failed to load posts: ${
                        exception.message ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}
