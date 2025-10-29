package com.naijaayo.worldwide

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.naijaayo.worldwide.theme.NigerianThemeManager
import com.naijaayo.worldwide.sound.SoundPreferencesManager
import com.naijaayo.worldwide.sound.MusicSettingsFragment
import com.naijaayo.worldwide.sound.VolumeSettingsFragment

/**
 * Activity for managing sound settings and preferences with tabs
 */
class SoundSettingsActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize theme manager and apply current theme
        NigerianThemeManager.initialize(this)
        NigerianThemeManager.applyThemeToActivity(this)

        // Hide action bar
        supportActionBar?.hide()

        setContentView(R.layout.activity_sound_settings)

        // Initialize the preferences manager
        SoundPreferencesManager.initialize(this)

        // Initialize views
        initializeViews()

        // Setup ViewPager with adapter
        setupViewPager()
    }

    private fun initializeViews() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
    }

    private fun setupViewPager() {
        val adapter = SoundSettingsPagerAdapter(this)
        viewPager.adapter = adapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Music"
                1 -> "Volume"
                else -> "Tab"
            }
        }.attach()
    }

    /**
     * Get current sound enabled state
     */
    fun isSoundEnabled(): Boolean {
        return SoundPreferencesManager.isSoundEnabled()
    }

    /**
     * Get current master volume (0.0f to 1.0f)
     */
    fun getMasterVolume(): Float {
        return SoundPreferencesManager.getMasterVolumeFloat()
    }

    override fun onResume() {
         super.onResume()
         // Reapply theme when activity becomes visible
         NigerianThemeManager.applyThemeToActivity(this)
         // Resume background music
         com.naijaayo.worldwide.sound.BackgroundMusicManager.resumeBackgroundMusic()
     }

    private inner class SoundSettingsPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): androidx.fragment.app.Fragment {
            return when (position) {
                0 -> MusicSettingsFragment()
                1 -> VolumeSettingsFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }
}
