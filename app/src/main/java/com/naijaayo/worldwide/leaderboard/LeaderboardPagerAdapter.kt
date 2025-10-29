package com.naijaayo.worldwide.leaderboard

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class LeaderboardPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SinglePlayerLeaderboardFragment()
            1 -> MultiplayerLeaderboardFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
