package com.example.instagramclone.data.repository

import com.example.instagramclone.data.model.Comment
import com.example.instagramclone.data.model.Post
import com.example.instagramclone.data.model.User
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    fun saveUserProfile(
        user: User,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("users")
            .document(user.uid)
            .set(user)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun savePost(
        post: Post,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("posts")
            .document(post.postId)
            .set(post)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }


    fun getPosts(
        onSuccess: (List<Post>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("posts")
            .get()
            .addOnSuccessListener { result ->

                val postList = mutableListOf<Post>()

                for (document in result.documents) {
                    val post = document.toObject(Post::class.java)

                    if (post != null) {
                        postList.add(
                            post.copy(
                                postId = post.postId.ifEmpty {
                                    document.id
                                }
                            )
                        )
                    }
                }

                postList.sortByDescending { it.createdAt }

                onSuccess(postList)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun getHomeFeedPosts(
        currentUid: String,
        onSuccess: (List<Post>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val authorIds = mutableSetOf<String>()
        authorIds.add(currentUid)

        var completedRequests = 0
        var hasFailed = false

        fun finishRelationshipRequest() {
            completedRequests++

            if (completedRequests == 2 && !hasFailed) {
                loadPostsForAuthors(
                    authorIds.toList(),
                    onSuccess,
                    onFailure
                )
            }
        }


        db.collection("following")
            .document(currentUid)
            .collection("userFollowing")
            .get()
            .addOnSuccessListener { snapshot ->

                for (document in snapshot.documents) {
                    authorIds.add(document.id)
                }

                finishRelationshipRequest()
            }
            .addOnFailureListener { exception ->
                if (!hasFailed) {
                    hasFailed = true
                    onFailure(exception)
                }
            }


        db.collection("followers")
            .document(currentUid)
            .collection("userFollowers")
            .get()
            .addOnSuccessListener { snapshot ->

                for (document in snapshot.documents) {
                    authorIds.add(document.id)
                }

                finishRelationshipRequest()
            }
            .addOnFailureListener { exception ->
                if (!hasFailed) {
                    hasFailed = true
                    onFailure(exception)
                }
            }
    }


    private fun loadPostsForAuthors(
        authorIds: List<String>,
        onSuccess: (List<Post>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (authorIds.isEmpty()) {
            onSuccess(emptyList())
            return
        }

        val allPosts = mutableMapOf<String, Post>()
        val chunks = authorIds.distinct().chunked(30)

        var completedQueries = 0
        var hasFailed = false

        for (chunk in chunks) {
            db.collection("posts")
                .whereIn("authorId", chunk)
                .get()
                .addOnSuccessListener { snapshot ->

                    if (hasFailed) {
                        return@addOnSuccessListener
                    }

                    for (document in snapshot.documents) {
                        val post = document.toObject(Post::class.java)

                        if (post != null) {
                            val postId = post.postId.ifEmpty {
                                document.id
                            }

                            allPosts[postId] = post.copy(
                                postId = postId
                            )
                        }
                    }

                    completedQueries++

                    if (completedQueries == chunks.size) {
                        val sortedPosts = allPosts.values
                            .sortedByDescending { it.createdAt }

                        onSuccess(sortedPosts)
                    }
                }
                .addOnFailureListener { exception ->
                    if (!hasFailed) {
                        hasFailed = true
                        onFailure(exception)
                    }
                }
        }
    }


    fun checkIfPostLiked(
        postId: String,
        userId: String,
        onSuccess: (Boolean) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("posts")
            .document(postId)
            .collection("likes")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                onSuccess(document.exists())
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }


    fun togglePostLike(
        postId: String,
        userId: String,
        onSuccess: (Boolean, Long) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val postRef = db.collection("posts")
            .document(postId)

        val likeRef = postRef.collection("likes")
            .document(userId)

        db.runTransaction { transaction ->

            val postSnapshot = transaction.get(postRef)
            val likeSnapshot = transaction.get(likeRef)

            if (!postSnapshot.exists()) {
                throw IllegalStateException("Post not found")
            }

            val currentCount =
                postSnapshot.getLong("likeCount") ?: 0L

            val isNowLiked: Boolean
            val newCount: Long

            if (likeSnapshot.exists()) {

                transaction.delete(likeRef)

                newCount = (currentCount - 1L).coerceAtLeast(0L)
                isNowLiked = false
            } else {

                transaction.set(
                    likeRef,
                    hashMapOf(
                        "uid" to userId,
                        "createdAt" to System.currentTimeMillis()
                    )
                )

                newCount = currentCount + 1L
                isNowLiked = true
            }

            transaction.update(
                postRef,
                "likeCount",
                newCount
            )

            Pair(isNowLiked, newCount)

        }.addOnSuccessListener { result ->
            onSuccess(result.first, result.second)
        }.addOnFailureListener { exception ->
            onFailure(exception)
        }
    }



    fun getComments(
        postId: String,
        onSuccess: (List<Comment>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("createdAt")
            .get()
            .addOnSuccessListener { snapshot ->

                val comments = snapshot.documents.mapNotNull { document ->
                    document.toObject(Comment::class.java)?.copy(
                        commentId = document.id
                    )
                }

                onSuccess(comments)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun addComment(
        postId: String,
        authorId: String,
        text: String,
        onSuccess: (Comment) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val cleanText = text.trim()

        if (cleanText.isEmpty()) {
            onFailure(IllegalArgumentException("Comment cannot be empty"))
            return
        }

        val postRef = db.collection("posts")
            .document(postId)

        val commentRef = postRef.collection("comments")
            .document()

        val comment = Comment(
            commentId = commentRef.id,
            authorId = authorId,
            text = cleanText,
            createdAt = System.currentTimeMillis()
        )

        db.runTransaction { transaction ->

            val postSnapshot = transaction.get(postRef)

            if (!postSnapshot.exists()) {
                throw IllegalStateException("Post not found")
            }

            val currentCount =
                postSnapshot.getLong("commentCount") ?: 0L

            transaction.set(commentRef, comment)

            transaction.update(
                postRef,
                "commentCount",
                currentCount + 1L
            )

            comment

        }.addOnSuccessListener { savedComment ->
            onSuccess(savedComment)
        }.addOnFailureListener { exception ->
            onFailure(exception)
        }
    }



    fun getUserById(
        uid: String,
        onSuccess: (User?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                val user = if (document.exists()) {
                    document.toObject(User::class.java)
                } else {
                    null
                }

                onSuccess(user)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}
