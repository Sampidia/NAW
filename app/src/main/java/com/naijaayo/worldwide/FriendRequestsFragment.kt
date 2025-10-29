package com.naijaayo.worldwide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FriendRequestsFragment : Fragment() {

    private val friendsViewModel: FriendsViewModel by activityViewModels()
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var friendRequestsRecyclerView: RecyclerView
    private lateinit var searchAdapter: SearchUserAdapter
    private lateinit var friendRequestsAdapter: FriendRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_friend_requests, container, false)

        searchEditText = view.findViewById(R.id.searchEditText)
        searchButton = view.findViewById(R.id.searchButton)
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView)
        friendRequestsRecyclerView = view.findViewById(R.id.friendRequestsRecyclerView)

        setupSearch()
        setupRecyclerViews()
        observeViewModel()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadFriendRequests()
    }

    private fun setupSearch() {
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                friendsViewModel.searchUsers(query)
            }
        }
    }

    private fun setupRecyclerViews() {
        searchAdapter = SearchUserAdapter(emptyList()) { user ->
            sendFriendRequest(user)
        }
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        searchResultsRecyclerView.adapter = searchAdapter

        friendRequestsAdapter = FriendRequestAdapter(emptyList()) { request, accept ->
            respondToFriendRequest(request, accept)
        }
        friendRequestsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        friendRequestsRecyclerView.adapter = friendRequestsAdapter
    }

    private fun observeViewModel() {
        friendsViewModel.searchResults.observe(viewLifecycleOwner, Observer { users ->
            searchAdapter.updateUsers(users)
        })

        friendsViewModel.friendRequests.observe(viewLifecycleOwner, Observer { requests ->
            friendRequestsAdapter.updateRequests(requests)
        })
    }

    private fun loadFriendRequests() {
        val currentUser = com.naijaayo.worldwide.auth.SessionManager.getCurrentUser()
        currentUser?.let {
            friendsViewModel.loadFriendRequests(it.id)
        }
    }

    private fun sendFriendRequest(user: User) {
        val currentUser = com.naijaayo.worldwide.auth.SessionManager.getCurrentUser()
        currentUser?.let {
            friendsViewModel.sendFriendRequest(it.id, user.id)
            searchEditText.text.clear()
            // Clear search results after sending request
            searchAdapter.updateUsers(emptyList())
        }
    }

    private fun respondToFriendRequest(request: FriendRequest, accept: Boolean) {
        val currentUser = com.naijaayo.worldwide.auth.SessionManager.getCurrentUser()
        currentUser?.let {
            friendsViewModel.respondToFriendRequest(request.id, it.id, accept)
        }
    }
}