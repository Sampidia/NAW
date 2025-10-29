package com.naijaayo.worldwide

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class FriendsPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MyFriendsFragment()
            1 -> FriendRequestsFragment()
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}