package com.naijaayo.worldwide

import com.naijaayo.worldwide.models.Users
import com.naijaayo.worldwide.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

class DatabaseService {

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
        }
    }

    // I will continue to add the other necessary functions in subsequent steps.
}
