package com.naijaayo.worldwide

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.naijaayo.worldwide.model.Friend
import com.naijaayo.worldwide.model.FriendRequest
import com.naijaayo.worldwide.model.User
import com.naijaayo.worldwide.network.FirebaseManager
import kotlinx.coroutines.launch

class FriendsViewModel : ViewModel() {

    private val _friends = MutableLiveData<List<Friend>>()
    val friends: LiveData<List<Friend>> = _friends

    private val _friendRequests = MutableLiveData<List<FriendRequest>>()
    val friendRequests: LiveData<List<FriendRequest>> = _friendRequests

    private val _searchResults = MutableLiveData<List<User>>()
    val searchResults: LiveData<List<User>> = _searchResults

    private var friendsListener: ListenerRegistration? = null
    private var requestsListener: ListenerRegistration? = null

    fun loadFriends(userId: String) {
        if (friendsListener != null) return // Already listening

        friendsListener = FirebaseManager.listenForFriends { friendsList ->
            _friends.postValue(friendsList)
        }
    }

    fun loadFriendRequests() {
        if (requestsListener != null) return // Already listening

        requestsListener = FirebaseManager.listenForFriendRequests { requests ->
            _friendRequests.postValue(requests)
        }
    }

    fun searchUsers(query: String) {
        android.util.Log.d("FriendsViewModel", "searchUsers called with query: '$query'")
        viewModelScope.launch {
            val results = FirebaseManager.searchUsers(query)
            android.util.Log.d("FriendsViewModel", "Posting ${results.size} results")
            _searchResults.postValue(results)
        }
    }

    fun sendFriendRequest(user: User, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = FirebaseManager.sendFriendRequest(user)
            onResult(success)
        }
    }

    fun acceptFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            FirebaseManager.acceptFriendRequest(request)
        }
    }

    fun declineFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            FirebaseManager.declineFriendRequest(request)
        }
    }

    override fun onCleared() {
        super.onCleared()
        friendsListener?.remove()
        requestsListener?.remove()
    }
}