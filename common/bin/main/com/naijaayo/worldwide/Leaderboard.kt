package com.naijaayo.worldwide

import java.io.Serializable

data class LeaderboardResponse(
    val singlePlayerLeaderboard: List<LeaderboardEntry>,
    val multiplayerLeaderboard: List<LeaderboardEntry>,
    val lastUpdated: String
) : Serializable

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val username: String,
    val avatarId: String,
    val rating: Int,
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Double
) : Serializable

data class UserStatsResponse(
    val userId: String,
    val username: String,
    val avatarId: String,
    val singlePlayerStats: PlayerStats,
    val multiplayerStats: PlayerStats,
    val lastActive: String
) : Serializable

data class PlayerStats(
    val rating: Int,
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Double,
    val averageScore: Double,
    val bestScore: Int,
    val currentStreak: Int,
    val longestStreak: Int
) : Serializable

// Extended User class with leaderboard stats
data class LeaderboardUser(
    val id: String,
    val username: String,
    val email: String,
    val avatarId: String = "ayo",
    val ratingSinglePlayer: Int = 1000,
    val ratingMultiplayer: Int = 1000,
    val winsSinglePlayer: Int = 0,
    val lossesSinglePlayer: Int = 0,
    val winsMultiplayer: Int = 0,
    val lossesMultiplayer: Int = 0,
    val gamesPlayedSinglePlayer: Int = 0,
    val gamesPlayedMultiplayer: Int = 0,
    val preferences: UserPreferences = UserPreferences(),
    val createdAt: String,
    val lastLoginAt: String? = null,
    val isOnline: Boolean = false
) : Serializable