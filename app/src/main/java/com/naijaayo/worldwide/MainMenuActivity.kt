package com.naijaayo.worldwide

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.naijaayo.worldwide.leaderboard.LeaderboardActivity

class MainMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        findViewById<Button>(R.id.singlePlayerButton).setOnClickListener {
            // Launch single-player game
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("isSinglePlayer", true)
            }
            startActivity(intent)
        }

        findViewById<Button>(R.id.multiplayerButton).setOnClickListener {
            startActivity(Intent(this, MultiplayerLobbyActivity::class.java))
        }

        findViewById<Button>(R.id.profileButton).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<Button>(R.id.leaderboardButton).setOnClickListener {
            startActivity(Intent(this, LeaderboardActivity::class.java))
        }

        findViewById<Button>(R.id.friendsButton).setOnClickListener {
            Toast.makeText(this, "Friends list coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
}
