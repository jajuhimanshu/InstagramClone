
package com.example.instagramclone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomeFragment : Fragment() {

    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter

    private val posts = mutableListOf<Post>()

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

        postsRecyclerView = view.findViewById(R.id.rvPosts)

        postAdapter = PostAdapter(
            posts = posts,
            onLikeClicked = { post ->
                Toast.makeText(
                    requireContext(),
                    "Like functionality will be connected next",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onCommentClicked = { post ->
                Toast.makeText(
                    requireContext(),
                    "Comments will be connected next",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        postsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext())
        postsRecyclerView.adapter = postAdapter
    }
}
