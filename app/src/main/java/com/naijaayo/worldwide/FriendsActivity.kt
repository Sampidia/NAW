package com.naijaayo.worldwide

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.naijaayo.worldwide.theme.NigerianThemeManager

class FriendsActivity : AppCompatActivity() {

    private val friendsViewModel: FriendsViewModel by viewModels()
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private val webSocketManager = com.naijaayo.worldwide.network.WebSocketManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize theme manager and apply current theme
        NigerianThemeManager.initialize(this)
        NigerianThemeManager.applyThemeToActivity(this)

        // Hide action bar
        supportActionBar?.hide()

        setContentView(R.layout.activity_friends)

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        setupViewPager()
        connectWebSocket()
    }

    private fun setupViewPager() {
        val adapter = FriendsPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "My Friends"
                1 -> "Friend Requests"
                else -> ""
            }
        }.attach()
    }

    private fun connectWebSocket() {
        val currentUser = com.naijaayo.worldwide.auth.SessionManager.getCurrentUser()
        currentUser?.let {
            webSocketManager.connect(it.id)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reapply theme when activity becomes visible
        NigerianThemeManager.applyThemeToActivity(this)
        // Resume background music
        com.naijaayo.worldwide.sound.BackgroundMusicManager.resumeBackgroundMusic()
        // Reconnect WebSocket if needed
        if (!webSocketManager.isConnected()) {
            connectWebSocket()
        }
    }

    override fun onPause() {
        super.onPause()
        // Disconnect WebSocket when activity is not visible
        webSocketManager.disconnect()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up WebSocket connection
        webSocketManager.disconnect()
    }
}