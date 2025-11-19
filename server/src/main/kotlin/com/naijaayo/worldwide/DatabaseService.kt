package com.naijaayo.worldwide

import com.naijaayo.worldwide.models.*
import com.naijaayo.worldwide.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.javatime.timestamp

class DatabaseService {

    // User operations
    private fun toUser(row: ResultRow): AuthUser = AuthUser(
        id = row[Users.id],
        username = row[Users.username],
        email = row[Users.email],
        avatarId = row[Users.avatarId]
    )

    suspend fun getUserById(id: String): AuthUser? = dbQuery {
        Users.select { Users.id eq id }
            .map(::toUser)
            .singleOrNull()
    }

    suspend fun getUserByUsername(username: String): AuthUser? = dbQuery {
        Users.select { Users.username eq username }
            .map(::toUser)
            .singleOrNull()
    }

    suspend fun getUserByEmail(email: String): AuthUser? = dbQuery {
        Users.select { Users.email eq email }
            .map(::toUser)
            .singleOrNull()
    }

    suspend fun getPasswordHash(userId: String): String? = dbQuery {
        Users.select { Users.id eq userId }
            .map { it[Users.passwordHash] }
            .singleOrNull()
    }

    suspend fun createUser(id: String, username: String, email: String, passwordHash: String) = dbQuery {
        Users.insert {
            it[Users.id] = id
            it[Users.username] = username
            it[Users.email] = email
            it[Users.passwordHash] = passwordHash
            it[Users.createdAt] = LocalDateTime.now()
        }
    }

    suspend fun updateUserLastSeen(id: String) = dbQuery {
        Users.update({ Users.id eq id }) {
            it[Users.lastSeen] = LocalDateTime.now()
            it[Users.isOnline] = true
        }
    }

    // Leaderboard operations
    suspend fun createLeaderboardEntry(entry: LeaderboardRecord) = dbQuery {
        Leaderboard.insert {
            it[Leaderboard.id] = entry.id
            it[Leaderboard.userId] = entry.userId
            it[Leaderboard.username] = entry.username
            it[Leaderboard.avatarId] = entry.avatarId
            it[Leaderboard.gameMode] = entry.gameMode
            it[Leaderboard.eloRating] = entry.eloRating
            it[Leaderboard.gamesPlayed] = entry.gamesPlayed
            it[Leaderboard.gamesWon] = entry.gamesWon
            it[Leaderboard.gamesLost] = entry.gamesLost
            it[Leaderboard.gamesDrawn] = entry.gamesDrawn
            it[Leaderboard.winStreak] = entry.winStreak
            it[Leaderboard.bestWinStreak] = entry.bestWinStreak
            it[Leaderboard.lastPlayed] = entry.lastPlayed
        }
    }

    // TODO: Add remaining CRUD operations for friends, messages, saved games, rooms, game results
}
