package com.naijaayo.worldwide.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naijaayo.worldwide.Room
import com.naijaayo.worldwide.repository.GameRepository
import kotlinx.coroutines.launch

class LobbyViewModel : ViewModel() {

    private val repository = GameRepository

    private val _rooms = MutableLiveData<List<Room>>()
    val rooms: LiveData<List<Room>> = _rooms

    fun fetchRooms() {
        viewModelScope.launch {
            try {
                _rooms.value = repository.getRooms()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addRoom(room: Room) {
        val currentRooms = _rooms.value?.toMutableList() ?: mutableListOf()
        currentRooms.add(room)
        _rooms.value = currentRooms
    }
}
