package com.naijaayo.worldwide

import com.naijaayo.worldwide.models.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

object DatabaseFactory {

    fun init() {
        val dbUri = URI(System.getenv("DATABASE_URL"))

        val userInfo = dbUri.userInfo.split(":")
        val username = userInfo[0]
        val password = userInfo[1]
        val dbUrl = "jdbc:postgresql://" + dbUri.host + ':' + dbUri.port + dbUri.path + "?sslmode=require"

        val dataSource = hikari(dbUrl, username, password)
        Database.connect(dataSource)

        // Create tables if they don't exist
        transaction {
            SchemaUtils.create(
                Users,
                Friends,
                FriendRequests,
                Messages,
                SavedGames,
                Leaderboard,
                Rooms,
                GameResults
            )
        }

        println("Database connection initialized and schema created.")
    }

    private fun hikari(url: String, user: String, pass: String): HikariDataSource {
        val config = HikariConfig()
        config.jdbcUrl = url
        config.username = user
        config.password = pass
        config.driverClassName = "org.postgresql.Driver"
        config.maximumPoolSize = 10
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
