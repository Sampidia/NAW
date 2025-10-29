package com.naijaayo.worldwide

import android.os.Bundle
import android.widget.Button
import com.naijaayo.worldwide.theme.NigerianThemeManager
import com.naijaayo.worldwide.auth.SessionManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.naijaayo.worldwide.game.GameViewModel

class ProfileActivity : AppCompatActivity() {

    private val gameViewModel: GameViewModel by viewModels()
    private lateinit var tabLayout: TabLayout
    private lateinit var characterPortrait: ImageView
    private lateinit var characterFullBody: ImageView
    private lateinit var brainMeter: ProgressBar
    private lateinit var eyeMeter: ProgressBar
    private lateinit var communicationMeter: ProgressBar
    private lateinit var confirmButton: Button

    private var selectedAvatarId: String = "ayo" // Default selection

    // Placeholder data for character stats
    private val characterStats = mapOf(
        "ayo" to mapOf("brain" to 80, "eye" to 70, "communication" to 90),
        "ada" to mapOf("brain" to 75, "eye" to 85, "communication" to 80),
        "fatima" to mapOf("brain" to 90, "eye" to 65, "communication" to 85)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize theme manager and apply current theme
        NigerianThemeManager.initialize(this)
        NigerianThemeManager.applyThemeToActivity(this)

        // Initialize avatar preference manager
        com.naijaayo.worldwide.theme.AvatarPreferenceManager.initialize(this)

        // Initialize session manager
        SessionManager.initialize(this)

        // Hide action bar to show only the logo image
        supportActionBar?.hide()

        setContentView(R.layout.activity_profile)

        tabLayout = findViewById(R.id.tabLayout)
        characterPortrait = findViewById(R.id.characterPortrait)
        characterFullBody = findViewById(R.id.characterFullBody)
        brainMeter = findViewById(R.id.brain_meter)
        eyeMeter = findViewById(R.id.eye_meter)
        communicationMeter = findViewById(R.id.communication_meter)
        confirmButton = findViewById(R.id.confirmButton)

        setupTabs()
        setupConfirmButton()
    }

    private fun setupTabs() {
        // Programmatically add tabs
        tabLayout.addTab(tabLayout.newTab().setText("Ayo"))
        tabLayout.addTab(tabLayout.newTab().setText("Ada"))
        tabLayout.addTab(tabLayout.newTab().setText("Fatima"))

        updateCharacter("ayo")

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val characterId = when (tab?.position) {
                    0 -> "ayo"
                    1 -> "ada"
                    2 -> "fatima"
                    else -> "ayo"
                }
                selectedAvatarId = characterId
                updateCharacter(characterId)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateCharacter(characterId: String) {
        val portraitResId = when (characterId) {
            "ayo" -> R.drawable.char_ayo_portrait
            "ada" -> R.drawable.char_ada_portrait
            "fatima" -> R.drawable.char_fatima_portrait
            else -> R.drawable.char_ayo_portrait
        }
        val fullBodyResId = when (characterId) {
            "ayo" -> R.drawable.char_ayo_full
            "ada" -> R.drawable.char_ada_full
            "fatima" -> R.drawable.char_fatima_full
            else -> R.drawable.char_ayo_full
        }

        characterPortrait.setImageResource(portraitResId)
        characterFullBody.setImageResource(fullBodyResId)

        // Update stats
        val stats = characterStats[characterId]
        stats?.let {
            brainMeter.progress = it["brain"] ?: 0
            eyeMeter.progress = it["eye"] ?: 0
            communicationMeter.progress = it["communication"] ?: 0
        }
    }

    private fun setupConfirmButton() {
        confirmButton.setOnClickListener {
            // Update local preferences
            gameViewModel.updateUserAvatar(selectedAvatarId)

            // Update session if user is logged in
            SessionManager.updateAvatar(selectedAvatarId)

            // TODO: Sync with server if user is authenticated
            val currentUser = SessionManager.getCurrentUser()
            if (currentUser != null) {
                // TODO: Make API call to update avatar on server
                // For now, just show success message
                Toast.makeText(this, "Avatar saved!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Avatar saved locally!", Toast.LENGTH_SHORT).show()
            }

            // Notify MainActivity to refresh avatar if it's running
            // This ensures the game screen updates with the new avatar
            finish()
        }
    }

    override fun onResume() {
         super.onResume()
         // Reapply theme when activity becomes visible (e.g., after returning from theme settings)
         NigerianThemeManager.applyThemeToActivity(this)
         // Resume background music
         com.naijaayo.worldwide.sound.BackgroundMusicManager.resumeBackgroundMusic()
     }
}
