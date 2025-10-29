package com.naijaayo.worldwide

import kotlin.math.pow
import kotlin.math.round

class LeaderboardService {

    fun calculateNewSinglePlayerRating(currentRating: Int, playerWon: Boolean): Int {
        // AI difficulty factor (can be adjusted)
        val aiStrength = 1000 // Base AI rating

        val expectedScore = 1.0 / (1.0 + 10.0.pow((aiStrength - currentRating) / 400.0))
        val actualScore = if (playerWon) 1.0 else 0.0

        val kFactor = when (currentRating) {
            in 0..1199 -> 32
            in 1200..1799 -> 24
            else -> 16
        }

        val newRating = currentRating + kFactor * (actualScore - expectedScore)
        return round(newRating).toInt()
    }

    fun calculateNewMultiplayerRatings(player1Rating: Int, player2Rating: Int, player1Won: Boolean): Pair<Int, Int> {
        val expectedScore1 = 1.0 / (1.0 + 10.0.pow((player2Rating - player1Rating) / 400.0))
        val expectedScore2 = 1.0 / (1.0 + 10.0.pow((player1Rating - player2Rating) / 400.0))

        val actualScore1 = if (player1Won) 1.0 else 0.0
        val actualScore2 = 1.0 - actualScore1

        val kFactor1 = getKFactor(player1Rating)
        val kFactor2 = getKFactor(player2Rating)

        val newRating1 = round(player1Rating + kFactor1 * (actualScore1 - expectedScore1)).toInt()
        val newRating2 = round(player2Rating + kFactor2 * (actualScore2 - expectedScore2)).toInt()

        return Pair(newRating1, newRating2)
    }

    private fun getKFactor(rating: Int): Int {
        return when {
            rating < 1200 -> 32
            rating < 1800 -> 24
            else -> 16
        }
    }

    fun generateLeaderboard(users: Map<String, LeaderboardUser>): LeaderboardResponse {
        val userList = users.values.toList()

        // Single player leaderboard
        val singlePlayerLeaderboard = userList
            .filter { it.gamesPlayedSinglePlayer > 0 }
            .sortedByDescending { it.ratingSinglePlayer }
            .take(50)
            .mapIndexed { index, user ->
                LeaderboardEntry(
                    rank = index + 1,
                    userId = user.id,
                    username = user.username,
                    avatarId = user.avatarId,
                    rating = user.ratingSinglePlayer,
                    gamesPlayed = user.gamesPlayedSinglePlayer,
                    wins = user.winsSinglePlayer,
                    losses = user.lossesSinglePlayer,
                    winRate = if (user.gamesPlayedSinglePlayer > 0) {
                        (user.winsSinglePlayer.toDouble() / user.gamesPlayedSinglePlayer) * 100
                    } else 0.0
                )
            }

        // Multiplayer leaderboard
        val multiplayerLeaderboard = userList
            .filter { it.gamesPlayedMultiplayer > 0 }
            .sortedByDescending { it.ratingMultiplayer }
            .take(50)
            .mapIndexed { index, user ->
                LeaderboardEntry(
                    rank = index + 1,
                    userId = user.id,
                    username = user.username,
                    avatarId = user.avatarId,
                    rating = user.ratingMultiplayer,
                    gamesPlayed = user.gamesPlayedMultiplayer,
                    wins = user.winsMultiplayer,
                    losses = user.lossesMultiplayer,
                    winRate = if (user.gamesPlayedMultiplayer > 0) {
                        (user.winsMultiplayer.toDouble() / user.gamesPlayedMultiplayer) * 100
                    } else 0.0
                )
            }

        return LeaderboardResponse(
            singlePlayerLeaderboard = singlePlayerLeaderboard,
            multiplayerLeaderboard = multiplayerLeaderboard,
            lastUpdated = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
        )
    }

    fun getUserStats(userId: String, users: Map<String, LeaderboardUser>): UserStatsResponse? {
        val user = users[userId] ?: return null

        val singlePlayerStats = PlayerStats(
            rating = user.ratingSinglePlayer,
            gamesPlayed = user.gamesPlayedSinglePlayer,
            wins = user.winsSinglePlayer,
            losses = user.lossesSinglePlayer,
            winRate = if (user.gamesPlayedSinglePlayer > 0) {
                (user.winsSinglePlayer.toDouble() / user.gamesPlayedSinglePlayer) * 100
            } else 0.0,
            averageScore = 0.0, // TODO: Calculate from game history
            bestScore = 0, // TODO: Calculate from game history
            currentStreak = 0, // TODO: Implement streak tracking
            longestStreak = 0 // TODO: Implement streak tracking
        )

        val multiplayerStats = PlayerStats(
            rating = user.ratingMultiplayer,
            gamesPlayed = user.gamesPlayedMultiplayer,
            wins = user.winsMultiplayer,
            losses = user.lossesMultiplayer,
            winRate = if (user.gamesPlayedMultiplayer > 0) {
                (user.winsMultiplayer.toDouble() / user.gamesPlayedMultiplayer) * 100
            } else 0.0,
            averageScore = 0.0, // TODO: Calculate from game history
            bestScore = 0, // TODO: Calculate from game history
            currentStreak = 0, // TODO: Implement streak tracking
            longestStreak = 0 // TODO: Implement streak tracking
        )

        return UserStatsResponse(
            userId = user.id,
            username = user.username,
            avatarId = user.avatarId,
            singlePlayerStats = singlePlayerStats,
            multiplayerStats = multiplayerStats,
            lastActive = user.lastLoginAt ?: user.createdAt
        )
    }
}