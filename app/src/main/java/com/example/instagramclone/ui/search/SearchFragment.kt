package com.example.instagramclone.ui.search

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.instagramclone.ui.profile.ProfileFragment
import com.example.instagramclone.R
import com.example.instagramclone.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment(R.layout.fragment_search) {

    private lateinit var searchAdapter: SearchAdapter
    private val db = FirebaseFirestore.getInstance()
    private val searchResultsList = mutableListOf<User>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvSearchUserList)
        val searchView = view.findViewById<SearchView>(R.id.searchView)

        searchView.setOnClickListener { searchView.isIconified = false }

        searchAdapter = SearchAdapter(searchResultsList) { clickedUser ->
            val profileFragment = ProfileFragment()
            val bundle = Bundle()
            bundle.putString("userId", clickedUser.uid)
            profileFragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, profileFragment)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = searchAdapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrBlank()) {
                    searchUsersInFirestore(newText.trim().lowercase())
                } else {
                    searchResultsList.clear()
                    searchAdapter.updateList(emptyList())
                }
                return true
            }
        })
    }

    private fun searchUsersInFirestore(searchText: String) {
        db.collection("users")
            .orderBy("usernameLowercase")
            .startAt(searchText)
            .endAt(searchText + "\uf8ff")
            .get()
            .addOnSuccessListener { snapshots ->
                val usersFound = mutableListOf<User>()

                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                for (document in snapshots.documents) {
                    val user = document.toObject(User::class.java)
                    if (user != null && document.id != currentUid) {
                        val userWithId = User(
                            uid = document.id,
                            username = user.username,
                            usernameLowercase = user.usernameLowercase,
                            displayName = user.displayName,
                            bio = user.bio,
                            profileImageUrl = user.profileImageUrl,
                            createdAt = user.createdAt
                        )
                        usersFound.add(userWithId)
                    }
                }

                searchAdapter.updateList(usersFound)
            }
            .addOnFailureListener { exception -> exception.printStackTrace() }
    }
}
