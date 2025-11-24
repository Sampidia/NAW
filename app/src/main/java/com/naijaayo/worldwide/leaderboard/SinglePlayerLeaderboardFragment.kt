package com.naijaayo.worldwide.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.R
import com.naijaayo.worldwide.network.FirebaseManager
import kotlinx.coroutines.launch

class SinglePlayerLeaderboardFragment : Fragment() {

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

        fetchLeaderboard()
    }

    private fun fetchLeaderboard() {
        viewLifecycleOwner.lifecycleScope.launch {
            val leaderboardEntries = FirebaseManager.getSinglePlayerLeaderboard()
            if (leaderboardEntries.isNotEmpty()) {
                adapter = LeaderboardAdapter(leaderboardEntries)
                recyclerView.adapter = adapter
            } else {
                Toast.makeText(context, "Single-player leaderboard is empty.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
