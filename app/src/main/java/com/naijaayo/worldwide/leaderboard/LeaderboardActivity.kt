package com.naijaayo.worldwide.leaderboard

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.R
import com.naijaayo.worldwide.game.GameViewModel

class LeaderboardActivity : AppCompatActivity() {

    private val gameViewModel: GameViewModel by viewModels()
    private lateinit var leaderboardRecyclerView: RecyclerView
    private lateinit var leaderboardAdapter: LeaderboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        leaderboardRecyclerView = findViewById(R.id.leaderboardRecyclerView)
        leaderboardRecyclerView.layoutManager = LinearLayoutManager(this)

        gameViewModel.leaderboard.observe(this) { users ->
            leaderboardAdapter = LeaderboardAdapter(users)
            leaderboardRecyclerView.adapter = leaderboardAdapter
        }

        gameViewModel.fetchLeaderboard()
    }
}
