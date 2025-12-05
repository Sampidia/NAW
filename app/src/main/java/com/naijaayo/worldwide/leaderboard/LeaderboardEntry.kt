package com.naijaayo.worldwide.leaderboard

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class LeaderboardEntry(
    val id: String = "",
    val displayName: String = "",
    val username: String = "",
    val avatarId: String = "ayo",
    val totalPoints: Long = 0,
    val wins: Long = 0,
    val losses: Long = 0,
    val draws: Long = 0
) {
    val games: Long get() = wins + losses + draws
    val winRate: Int get() = if (games > 0) ((wins * 100) / games).toInt() else 0
}
