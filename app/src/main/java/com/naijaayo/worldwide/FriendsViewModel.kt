package com.naijaayo.worldwide

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naijaayo.worldwide.model.Friend
import kotlinx.coroutines.launch

class FriendsViewModel : ViewModel() {

    private val _friends = MutableLiveData<List<Friend>>()
    val friends: LiveData<List<Friend>> = _friends

    fun loadFriends(userId: String) {
        // TODO: Implement friend loading logic
        viewModelScope.launch {
            // For now, emit empty list
            _friends.value = emptyList()
        }
    }
}