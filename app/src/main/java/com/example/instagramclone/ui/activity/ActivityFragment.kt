package com.example.instagramclone.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.instagramclone.R
import com.example.instagramclone.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth

class ActivityFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val repository = NotificationRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_activity,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        recyclerView = view.findViewById(R.id.rvNotifications)
        adapter = NotificationAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        loadNotifications()
    }

    private fun loadNotifications() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Log.d(
            "ACTIVITY_DEBUG",
            "Current User = $uid"
        )
        repository.getNotifications(
            uid,
            onSuccess = {
                Log.d(
                    "ACTIVITY_CHECK",
                    "Notifications Count = ${it.size}"
                )
                adapter.updateNotifications(it)
            },
            onFailure = {
            }
        )
    }
}