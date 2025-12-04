package com.naijaayo.worldwide

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class FriendsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)
        supportActionBar?.hide()

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val backButton = findViewById<ImageView>(R.id.logoImageView) // Using logo as back button for now or finding correct ID
        val fabSearchUser = findViewById<FloatingActionButton>(R.id.fabSearchUser) // This ID needs to be in XML

        val adapter = FriendsPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "My Friends"
                1 -> "Requests"
                else -> null
            }
        }.attach()

        // Note: activity_friends.xml uses logoImageView, not backButton. 
        // If we want a back button, we should add it or use the logo.
        // For now, I'll make the logo close the activity if clicked.
        backButton.setOnClickListener {
            finish()
        }

        fabSearchUser.setOnClickListener {
            val dialog = SearchFriendsDialog()
            dialog.show(supportFragmentManager, "SearchFriendsDialog")
        }
    }
}
