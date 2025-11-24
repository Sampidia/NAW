package com.naijaayo.worldwide.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.ValueEventListener
import com.naijaayo.worldwide.network.FirebaseManager
import com.naijaayo.worldwide.network.Game

class LobbyViewModel : ViewModel() {

    private val _rooms = MutableLiveData<List<Game>>()
    val rooms: LiveData<List<Game>> = _rooms

    private var roomsListener: ValueEventListener? = null

    fun listenForRooms() {
        roomsListener = FirebaseManager.listenForPublicRooms { rooms ->
            _rooms.value = rooms
        }
    }

    override fun onCleared() {
        super.onCleared()
        roomsListener?.let { FirebaseManager.removeListener(it) }
    }
}
