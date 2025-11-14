package com.naijaayo.worldwide

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.auth.jwt.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

data class AuthUser(
    val id: String,
    val username: String,
    val email: String,
    val avatarId: String
)

class AuthService {
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "your-secret-key-change-in-production"
    private val jwtIssuer = "naija-ayo-worldwide"
    private val jwtAudience = "naija-ayo-users"
    private val algorithm = Algorithm.HMAC256(jwtSecret)

    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }

    fun generateToken(user: AuthUser): String {
        return JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withClaim("userId", user.id)
            .withClaim("username", user.username)
            .withClaim("email", user.email)
            .withClaim("avatarId", user.avatarId)
            .withExpiresAt(Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)) // 7 days
            .sign(algorithm)
    }

    fun validateToken(token: String): AuthUser? {
        return try {
            val verifier = JWT.require(algorithm)
                .withAudience(jwtAudience)
                .withIssuer(jwtIssuer)
                .build()

            val decoded = verifier.verify(token)
            AuthUser(
                id = decoded.getClaim("userId").asString(),
                username = decoded.getClaim("username").asString(),
                email = decoded.getClaim("email").asString(),
                avatarId = decoded.getClaim("avatarId").asString()
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun registerUser(username: String, email: String, password: String): Result<AuthUser> {
        return try {
            val passwordHash = hashPassword(password)
            val userId = generateId()

            transaction {
                // Check if username or email already exists
                val existingUser = Users.select {
                    (Users.username eq username) or (Users.email eq email)
                }.singleOrNull()

                if (existingUser != null) {
                    throw Exception("Username or email already exists")
                }

                // Create user
                Users.insert {
                    it[id] = userId
                    it[Users.username] = username
                    it[Users.email] = email
                    it[Users.passwordHash] = passwordHash
                    it[avatarId] = "ayo"
                }

                // Create leaderboard entry
                Leaderboard.insert {
                    it[id] = generateId()
                    it[Leaderboard.userId] = userId
                    it[Leaderboard.username] = username
                    it[Leaderboard.avatarId] = "ayo"
                    it[gameMode] = "single_player"
                }

                Leaderboard.insert {
                    it[id] = generateId()
                    it[Leaderboard.userId] = userId
                    it[Leaderboard.username] = username
                    it[Leaderboard.avatarId] = "ayo"
                    it[gameMode] = "multiplayer"
                }
            }

            Result.success(AuthUser(userId, username, email, "ayo"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(emailOrUsername: String, password: String): Result<AuthUser> {
        return try {
            val user = transaction {
                Users.select {
                    (Users.username eq emailOrUsername) or (Users.email eq emailOrUsername)
                }.singleOrNull()
            }

            if (user == null) {
                return Result.failure(Exception("User not found"))
            }

            val passwordHash = user[Users.passwordHash]
            if (!verifyPassword(password, passwordHash)) {
                return Result.failure(Exception("Invalid password"))
            }

            // Update last seen
            transaction {
                Users.update({ Users.id eq user[Users.id] }) {
                    it[lastSeen] = LocalDateTime.now()
                    it[isOnline] = true
                }
            }

            Result.success(AuthUser(
                id = user[Users.id],
                username = user[Users.username],
                email = user[Users.email],
                avatarId = user[Users.avatarId]
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser(call: io.ktor.server.application.ApplicationCall): AuthUser? {
        return try {
            val principal = call.principal<JWTPrincipal>()
            principal?.let {
                AuthUser(
                    id = it.getClaim("userId", String::class) ?: return null,
                    username = it.getClaim("username", String::class) ?: return null,
                    email = it.getClaim("email", String::class) ?: return null,
                    avatarId = it.getClaim("avatarId", String::class) ?: return null
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getUserById(userId: String): AuthUser? {
        return try {
            transaction {
                Users.select { Users.id eq userId }
                    .singleOrNull()
                    ?.let {
                        AuthUser(
                            id = it[Users.id],
                            username = it[Users.username],
                            email = it[Users.email],
                            avatarId = it[Users.avatarId]
                        )
                    }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun generateId(): String {
        return UUID.randomUUID().toString()
    }
}