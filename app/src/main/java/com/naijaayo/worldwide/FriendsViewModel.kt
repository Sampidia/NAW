package com.naijaayo.worldwide

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naijaayo.worldwide.network.FriendsRepository
import com.naijaayo.worldwide.network.RetrofitClient
import kotlinx.coroutines.launch

class FriendsViewModel : ViewModel() {

    private val friendsRepository = FriendsRepository(RetrofitClient.friendsApiService)

    private val _friends = MutableLiveData<List<Friend>>()
    val friends: LiveData<List<Friend>> = _friends

    private val _friendRequests = MutableLiveData<List<FriendRequest>>()
    val friendRequests: LiveData<List<FriendRequest>> = _friendRequests

    private val _searchResults = MutableLiveData<List<User>>()
    val searchResults: LiveData<List<User>> = _searchResults

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadFriends(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = friendsRepository.loadFriends(userId)
                result.onSuccess { friends ->
                    _friends.value = friends
                }.onFailure { error ->
                    _error.value = "Failed to load friends: ${error.message}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFriendRequests(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = friendsRepository.loadFriendRequests(userId)
                result.onSuccess { requests ->
                    _friendRequests.value = requests
                }.onFailure { error ->
                    _error.value = "Failed to load friend requests: ${error.message}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = friendsRepository.searchUsers(query)
                result.onSuccess { users ->
                    _searchResults.value = users
                }.onFailure { error ->
                    _error.value = "Failed to search users: ${error.message}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendFriendRequest(fromUserId: String, toUserId: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                val currentUser = com.naijaayo.worldwide.auth.SessionManager.getCurrentUser()
                currentUser?.let { user ->
                    val request = FriendRequest(
                        id = generateId(),
                        fromUserId = fromUserId,
                        toUserId = toUserId,
                        fromUsername = user.username,
                        fromEmail = user.email,
                        fromAvatarId = user.avatarId
                    )

                    val result = friendsRepository.sendFriendRequest(fromUserId, request)
                    result.onSuccess {
                        // Reload friend requests after sending
                        loadFriendRequests(fromUserId)
                    }.onFailure { error ->
                        _error.value = "Failed to send friend request: ${error.message}"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            }
        }
    }

    fun respondToFriendRequest(requestId: String, userId: String, accept: Boolean) {
        viewModelScope.launch {
            _error.value = null
            try {
                val result = friendsRepository.respondToFriendRequest(userId, requestId, accept)
                result.onSuccess {
                    // Reload friends and requests after response
                    loadFriends(userId)
                    loadFriendRequests(userId)
                }.onFailure { error ->
                    _error.value = "Failed to respond to friend request: ${error.message}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            }
        }
    }

    fun sendMessage(message: Message) {
        viewModelScope.launch {
            _error.value = null
            try {
                val result = friendsRepository.sendMessage(message.fromUserId, message)
                result.onSuccess {
                    // Reload messages after sending
                    loadMessages(message.fromUserId, message.toUserId)
                }.onFailure { error ->
                    _error.value = "Failed to send message: ${error.message}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            }
        }
    }

    fun loadMessages(userId: String, friendId: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                val result = friendsRepository.loadMessages(userId, friendId)
                result.onSuccess { messages ->
                    _messages.value = messages.sortedBy { it.timestamp }
                }.onFailure { error ->
                    _error.value = "Failed to load messages: ${error.message}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            }
        }
    }

    private fun generateId(): String {
        return java.util.UUID.randomUUID().toString()
    }
}