package com.example.instagramclone.ui.comments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.instagramclone.R
import com.example.instagramclone.data.model.Comment
import com.example.instagramclone.data.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth

class CommentsFragment : Fragment() {

    private lateinit var commentsAdapter: CommentAdapter
    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentInput: EditText
    private lateinit var postCommentButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyMessage: TextView

    private val comments = mutableListOf<Comment>()
    private val repository = FirestoreRepository()

    private var postId: String = ""
    private var postOwnerId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postId = arguments?.getString(ARG_POST_ID).orEmpty()
        postOwnerId = arguments?.getString(ARG_POST_OWNER_ID).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_comments,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        commentsRecyclerView = view.findViewById(R.id.rvComments)
        commentInput = view.findViewById(R.id.etNewComment)
        postCommentButton = view.findViewById(R.id.btnPostComment)
        progressBar = view.findViewById(R.id.progressComments)
        emptyMessage = view.findViewById(R.id.tvCommentsEmpty)

        commentsAdapter = CommentAdapter(comments)

        commentsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        commentsRecyclerView.adapter = commentsAdapter

        postCommentButton.setOnClickListener {
            addNewComment()
        }

        if (postId.isBlank()) {
            Toast.makeText(
                requireContext(),
                "Unable to open comments for this post",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        loadComments()
    }

    private fun loadComments() {
        progressBar.visibility = View.VISIBLE
        emptyMessage.visibility = View.GONE

        repository.getComments(
            postId = postId,
            onSuccess = { loadedComments ->
                if (!isAdded) return@getComments

                progressBar.visibility = View.GONE
                commentsAdapter.updateComments(loadedComments)

                emptyMessage.visibility =
                    if (loadedComments.isEmpty()) View.VISIBLE
                    else View.GONE
            },
            onFailure = { exception ->
                if (!isAdded) return@getComments

                progressBar.visibility = View.GONE

                Toast.makeText(
                    requireContext(),
                    "Couldn't load comments: ${exception.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun addNewComment() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUid == null) {
            Toast.makeText(
                requireContext(),
                "Please log in to comment",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val text = commentInput.text.toString().trim()

        if (text.isEmpty()) {
            commentInput.error = "Enter a comment"
            return
        }

        postCommentButton.isEnabled = false

        repository.addComment(
            postId = postId,
            authorId = currentUid,
            postOwnerId = postOwnerId,
            text = text,
            onSuccess = { savedComment ->
                if (!isAdded) return@addComment

                comments.add(savedComment)
                commentsAdapter.updateComments(comments)

                commentInput.text.clear()
                postCommentButton.isEnabled = true
                emptyMessage.visibility = View.GONE

                val inputMethodManager =
                    requireContext().getSystemService(
                        Context.INPUT_METHOD_SERVICE
                    ) as InputMethodManager

                inputMethodManager.hideSoftInputFromWindow(
                    commentInput.windowToken,
                    0
                )

                commentsRecyclerView.scrollToPosition(comments.size - 1)
            },
            onFailure = { exception ->
                if (!isAdded) return@addComment

                postCommentButton.isEnabled = true

                Toast.makeText(
                    requireContext(),
                    "Couldn't post comment: ${exception.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    companion object {
        private const val ARG_POST_ID = "post_id"
        private const val ARG_POST_OWNER_ID = "post_owner_id"

        fun newInstance(postId: String,postOwnerId :String ): CommentsFragment {
            return CommentsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_POST_ID, postId)
                    putString(ARG_POST_OWNER_ID, postOwnerId)
                }
            }
        }
    }
}
