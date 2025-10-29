package com.naijaayo.worldwide.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.R
import com.naijaayo.worldwide.game.GameViewModel

class MultiplayerLeaderboardFragment : Fragment() {

    private val gameViewModel: GameViewModel by activityViewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LeaderboardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.leaderboardRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Observe leaderboard data
        gameViewModel.leaderboard.observe(viewLifecycleOwner) { users ->
            adapter = LeaderboardAdapter(users, isSinglePlayer = false)
            recyclerView.adapter = adapter
        }

        // Fetch leaderboard data
        gameViewModel.fetchLeaderboard()
    }
}
