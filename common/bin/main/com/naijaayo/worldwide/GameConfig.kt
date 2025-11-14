package com.naijaayo.worldwide

import java.io.Serializable

data class GameConfig(
    val maxPlayers: Int = 2,
    val turnTimeLimit: Int = 30 // in seconds
) : Serializable