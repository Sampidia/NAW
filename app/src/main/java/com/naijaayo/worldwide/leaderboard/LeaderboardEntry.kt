package com.naijaayo.worldwide.leaderboard

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class LeaderboardEntry(
    val displayName: String? = null,
    val score: Long = 0,
    val avatarId: String? = null,
    // The user's UID from Firebase Auth is used as the document ID
    // so it doesn't need to be a field in the document itself.
)
