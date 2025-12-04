package com.naijaayo.worldwide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FriendRequestsFragment : Fragment() {

    private val friendsViewModel: FriendsViewModel by activityViewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FriendRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_friend_requests, container, false)
        recyclerView = view.findViewById(R.id.friendRequestsRecyclerView)
        setupRecyclerView()
        observeViewModel()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        friendsViewModel.loadFriendRequests()
    }

    private fun setupRecyclerView() {
        adapter = FriendRequestAdapter(emptyList()) { request, accepted ->
            if (accepted) {
                friendsViewModel.acceptFriendRequest(request)
                Toast.makeText(context, "Friend request accepted", Toast.LENGTH_SHORT).show()
            } else {
                friendsViewModel.declineFriendRequest(request)
                Toast.makeText(context, "Friend request declined", Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        friendsViewModel.friendRequests.observe(viewLifecycleOwner) { requests ->
            adapter.updateRequests(requests)
        }
    }
}