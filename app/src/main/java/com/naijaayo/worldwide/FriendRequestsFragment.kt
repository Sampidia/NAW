package com.naijaayo.worldwide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FriendRequestsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FriendRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_friend_requests, container, false)

        recyclerView = view.findViewById(R.id.friendRequestsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = FriendRequestAdapter(emptyList()) { _, _ -> }
        recyclerView.adapter = adapter

        // TODO: Load friend requests from repository

        return view
    }
}