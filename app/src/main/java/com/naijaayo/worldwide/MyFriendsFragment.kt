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
import com.google.firebase.database.ValueEventListener
import com.naijaayo.worldwide.auth.SessionManager
import com.naijaayo.worldwide.model.Friend
import com.naijaayo.worldwide.ChatDialog
import com.naijaayo.worldwide.FriendsViewModel
import com.naijaayo.worldwide.network.FirebaseManager

class MyFriendsFragment : Fragment() {

    private val friendsViewModel: FriendsViewModel by activityViewModels()
    private lateinit var friendsRecyclerView: RecyclerView
    private lateinit var friendsAdapter: FriendAdapter
    
    // Map to track online status listeners by friend ID
    private val onlineStatusListeners = mutableMapOf<String, ValueEventListener>()

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
            // Setup online status listeners for each friend
            setupOnlineStatusListeners(friends)
        })
    }

    private fun loadFriends() {
        val currentUser = SessionManager.getCurrentUser()
        currentUser?.let {
            friendsViewModel.loadFriends(it.id)
        }
    }
    
    private fun setupOnlineStatusListeners(friends: List<Friend>) {
        // Remove old listeners that are no longer needed
        val currentFriendIds = friends.map { it.id }.toSet()
        val listenersToRemove = onlineStatusListeners.keys.filter { it !in currentFriendIds }
        listenersToRemove.forEach { friendId ->
            onlineStatusListeners[friendId]?.let { listener ->
                FirebaseManager.removeOnlineStatusListener(friendId, listener)
            }
            onlineStatusListeners.remove(friendId)
        }
        
        // Add listeners for new friends
        friends.forEach { friend ->
            if (friend.id !in onlineStatusListeners) {
                val listener = FirebaseManager.listenToUserOnlineStatus(friend.id) { isOnline ->
                    // Update adapter with new online status
                    activity?.runOnUiThread {
                        friendsAdapter.updateOnlineStatus(friend.id, isOnline)
                    }
                }
                onlineStatusListeners[friend.id] = listener
            }
        }
    }

    private fun openChatDialog(friend: Friend) {
        val chatDialog = ChatDialog(friend)
        chatDialog.show(parentFragmentManager, "ChatDialog")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up all online status listeners
        onlineStatusListeners.forEach { (friendId, listener) ->
            FirebaseManager.removeOnlineStatusListener(friendId, listener)
        }
        onlineStatusListeners.clear()
    }
}
