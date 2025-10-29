package com.naijaayo.worldwide.leaderboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.naijaayo.worldwide.R
import com.naijaayo.worldwide.theme.NigerianThemeManager

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var viewPager: androidx.viewpager2.widget.ViewPager2
    private lateinit var tabLayout: com.google.android.material.tabs.TabLayout
    private lateinit var pagerAdapter: LeaderboardPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize theme manager and apply current theme
        NigerianThemeManager.initialize(this)
        NigerianThemeManager.applyThemeToActivity(this)

        // Hide action bar to show only the logo image
        supportActionBar?.hide()

        setContentView(R.layout.activity_leaderboard)

        // Initialize views
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        // Set up ViewPager with adapter
        pagerAdapter = LeaderboardPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Single Player"
                1 -> "Multiplayer"
                else -> "Tab $position"
            }
        }.attach()
    }

    override fun onResume() {
         super.onResume()
         // Reapply theme when activity becomes visible (e.g., after returning from theme settings)
         NigerianThemeManager.applyThemeToActivity(this)
         // Resume background music
         com.naijaayo.worldwide.sound.BackgroundMusicManager.resumeBackgroundMusic()
     }
}
