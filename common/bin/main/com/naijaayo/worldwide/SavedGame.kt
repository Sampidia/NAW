package com.naijaayo.worldwide

import java.io.Serializable

data class SavedGame(
    val id: String,
    val userId: String,
    val gameMode: String, // "single_player" or "multiplayer"
    val difficulty: GameLevel,
    val gameState: GameState,
    val opponentId: String? = null,
    val opponentUsername: String? = null,
    val opponentAvatarId: String? = null,
    val isOpponentOnline: Boolean = false,
    val savedAt: String,
    val roomId: String? = null // For multiplayer games
) : Serializable

data class SaveGameRequest(
    val gameMode: String,
    val difficulty: GameLevel,
    val gameState: GameState,
    val opponentId: String? = null,
    val roomId: String? = null
) : Serializable

data class SaveGameResponse(
    val success: Boolean,
    val savedGame: SavedGame? = null,
    val message: String? = null
) : Serializable

data class LoadGameResponse(
    val success: Boolean,
    val savedGame: SavedGame? = null,
    val message: String? = null
) : Serializable

data class SavedGamesResponse(
    val success: Boolean,
    val savedGames: List<SavedGame> = emptyList(),
    val message: String? = null
) : Serializable