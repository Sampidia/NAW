package com.naijaayo.worldwide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MyFriendsFragment : Fragment() {

    private val friendsViewModel: FriendsViewModel by activityViewModels()
    private lateinit var friendsRecyclerView: RecyclerView
    private lateinit var friendsAdapter: FriendAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_friends, container, false)

        friendsRecyclerView = view.findViewById(R.id.friendsRecyclerView)

        setupRecyclerView()
        observeViewModel()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadFriends()
    }

    private fun setupRecyclerView() {
        friendsAdapter = FriendAdapter(emptyList()) { friend ->
            openChatDialog(friend)
        }

        friendsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        friendsRecyclerView.adapter = friendsAdapter
    }

    private fun observeViewModel() {
        friendsViewModel.friends.observe(viewLifecycleOwner, Observer { friends ->
            friendsAdapter.updateFriends(friends)
        })
    }

    private fun loadFriends() {
        val currentUser = com.naijaayo.worldwide.auth.SessionManager.getCurrentUser()
        currentUser?.let {
            friendsViewModel.loadFriends(it.id)
        }
    }

    private fun openChatDialog(friend: Friend) {
        val chatDialog = ChatDialog(friend)
        chatDialog.show(parentFragmentManager, "ChatDialog")
    }
}