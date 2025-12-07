package com.naijaayo.worldwide

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.naijaayo.worldwide.network.FirebaseManager
import com.naijaayo.worldwide.theme.AvatarPreferenceManager
import com.naijaayo.worldwide.theme.NigerianThemeManager
import android.view.MotionEvent
import android.view.animation.ScaleAnimation
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    // --- Views ---
    private lateinit var tabLayout: TabLayout
    private lateinit var characterPortrait: ImageView
    private lateinit var characterFullBody: ImageView
    private lateinit var brainMeter: ProgressBar
    private lateinit var eyeMeter: ProgressBar
    private lateinit var communicationMeter: ProgressBar
    private lateinit var confirmButton: Button
    private lateinit var authContainer: ConstraintLayout
    private lateinit var profileContainer: ConstraintLayout
    private lateinit var usernameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var authActionButton: Button
    private lateinit var toggleAuthMode: TextView
    private lateinit var forgotPasswordText: TextView
    private lateinit var logoutButton: Button
    private lateinit var authErrorText: TextView

    // --- State ---
    private var selectedAvatarId: String = "ayo"
    private var isLoginMode = true
    private lateinit var auth: FirebaseAuth

    private val characterStats = mapOf(
        "ayo" to mapOf("brain" to 80, "eye" to 70, "communication" to 90),
        "ada" to mapOf("brain" to 75, "eye" to 85, "communication" to 80),
        "fatima" to mapOf("brain" to 90, "eye" to 65, "communication" to 85)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NigerianThemeManager.initialize(this)
        NigerianThemeManager.applyThemeToActivity(this)
        AvatarPreferenceManager.initialize(this)
        supportActionBar?.hide()
        setContentView(R.layout.activity_profile)

        auth = FirebaseManager.auth

        initializeViews()
        setupTabs()
        setupAuthLogic()
        setupConfirmButton()

        setupHoverAnimations()
        updateAuthUI()

        // Load GIF
        Glide.with(this).load(R.raw.logo_animate).into(findViewById(R.id.appLogo))
    }

    override fun onResume() {
        super.onResume()
        NigerianThemeManager.applyThemeToActivity(this)
        com.naijaayo.worldwide.sound.BackgroundMusicManager.resumeBackgroundMusic()
        updateUI()
    }

    private fun initializeViews() {
        tabLayout = findViewById(R.id.tabLayout)
        characterPortrait = findViewById(R.id.characterPortrait)
        characterFullBody = findViewById(R.id.characterFullBody)
        brainMeter = findViewById(R.id.brain_meter)
        eyeMeter = findViewById(R.id.eye_meter)
        communicationMeter = findViewById(R.id.communication_meter)
        confirmButton = findViewById(R.id.confirmButton)
        profileContainer = findViewById(R.id.profileContainer)

        authContainer = findViewById(R.id.authContainer)
        usernameInput = findViewById(R.id.usernameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        authActionButton = findViewById(R.id.authActionButton)
        toggleAuthMode = findViewById(R.id.toggleAuthMode)
        forgotPasswordText = findViewById(R.id.forgotPasswordText)
        logoutButton = findViewById(R.id.logoutButton)
        authErrorText = findViewById(R.id.authErrorText)
    }

    private fun updateUI() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            authContainer.visibility = View.GONE
            profileContainer.visibility = View.VISIBLE
            logoutButton.visibility = View.VISIBLE
            loadUserProfile(currentUser.uid)
        } else {
            authContainer.visibility = View.VISIBLE
            profileContainer.visibility = View.GONE
            logoutButton.visibility = View.GONE
            val localAvatar = AvatarPreferenceManager.getUserAvatar()
            updateCharacter(localAvatar)
            selectTabForAvatar(localAvatar)
        }
    }

    private fun loadUserProfile(uid: String) {
        lifecycleScope.launch {
            val user = FirebaseManager.getUserProfile(uid)
            if (user != null) {
                val avatarId = user["avatarId"] as? String ?: "ayo"
                selectedAvatarId = avatarId
                updateCharacter(avatarId)
                selectTabForAvatar(avatarId)
            } else {
                Toast.makeText(this@ProfileActivity, "Failed to load profile.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAuthLogic() {
        authActionButton.setOnClickListener {
            authErrorText.visibility = View.GONE
            if (isLoginMode) {
                performLogin()
            } else {
                performRegistration()
            }
        }

        toggleAuthMode.setOnClickListener {
            isLoginMode = !isLoginMode
            authErrorText.visibility = View.GONE
            updateAuthUI()
        }

        forgotPasswordText.setOnClickListener {
            showForgotPasswordDialog()
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            updateUI()
        }
    }

    private fun showForgotPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_reset_password, null)
        val errorBanner = dialogView.findViewById<TextView>(R.id.errorBanner)
        val emailInput = dialogView.findViewById<EditText>(R.id.emailInput)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val sendButton = dialogView.findViewById<Button>(R.id.sendButton)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        sendButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                errorBanner.text = "Please enter an email"
                errorBanner.visibility = View.VISIBLE
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                errorBanner.text = "Invalid email"
                errorBanner.visibility = View.VISIBLE
            } else {
                errorBanner.visibility = View.GONE
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Reset link sent to $email", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        errorBanner.text = "Error: ${e.message}"
                        errorBanner.visibility = View.VISIBLE
                    }
            }
        }

        dialog.show()
    }

    private fun setupHoverAnimations() {
        val scaleDown = ScaleAnimation(1f, 0.95f, 1f, 0.95f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f)
        scaleDown.duration = 100
        scaleDown.fillAfter = true

        val scaleUp = ScaleAnimation(0.95f, 1f, 0.95f, 1f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f)
        scaleUp.duration = 100
        scaleUp.fillAfter = true

        val touchListener = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.startAnimation(scaleDown)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.startAnimation(scaleUp)
            }
            false
        }

        forgotPasswordText.setOnTouchListener(touchListener)
        toggleAuthMode.setOnTouchListener(touchListener)
    }

    private fun updateAuthUI() {
        if (isLoginMode) {
            emailInput.visibility = View.GONE
            usernameInput.hint = "Username or Email"
            authActionButton.text = "Log in"
            toggleAuthMode.text = "Don't have an account? Sign up"
            forgotPasswordText.visibility = View.VISIBLE
        } else {
            emailInput.visibility = View.VISIBLE
            usernameInput.hint = "Username"
            authActionButton.text = "Sign up"
            toggleAuthMode.text = "Already have an account? Log in"
            forgotPasswordText.visibility = View.GONE
        }
        // Hide error text when switching modes
        authErrorText.visibility = View.GONE
    }

    private fun performLogin() {
        val input = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (input.isNotEmpty() && password.isNotEmpty()) {
            lifecycleScope.launch {
                val success = FirebaseManager.loginUser(input, password)
                if (success) {
                    updateUI()
                } else {
                    authErrorText.text = "Invalid login details. Please check your username/email and password."
                    authErrorText.visibility = View.VISIBLE
                }
            }
        } else {
            Toast.makeText(baseContext, "Enter Username/Email and Password.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performRegistration() {
        val username = usernameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
            lifecycleScope.launch {
                val success = FirebaseManager.registerUser(email, password, username)
                if (success) {
                    auth.currentUser?.let { saveUserProfile(it.uid) }
                    updateUI()
                } else {
                    authErrorText.text = "Registration failed. Please try again."
                    authErrorText.visibility = View.VISIBLE
                }
            }
        } else {
            Toast.makeText(baseContext, "Please fill all fields.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Ayo"))
        tabLayout.addTab(tabLayout.newTab().setText("Ada"))
        tabLayout.addTab(tabLayout.newTab().setText("Fatima"))

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
        selectedAvatarId = characterId
        val portraitResId = AvatarPreferenceManager.getAvatarPortrait(characterId)
        val fullBodyResId = AvatarPreferenceManager.getAvatarFullBody(characterId)

        characterPortrait.setImageResource(portraitResId)
        characterFullBody.setImageResource(fullBodyResId)

        val stats = characterStats[characterId]
        stats?.let {
            brainMeter.progress = it["brain"] ?: 0
            eyeMeter.progress = it["eye"] ?: 0
            communicationMeter.progress = it["communication"] ?: 0
        }
    }
    
    private fun selectTabForAvatar(avatarId: String) {
        val tabIndex = when (avatarId) {
            "ayo" -> 0
            "ada" -> 1
            "fatima" -> 2
            else -> 0
        }
        tabLayout.getTabAt(tabIndex)?.select()
    }

    private fun saveUserProfile(uid: String) {
        lifecycleScope.launch {
            val success = FirebaseManager.saveUserAvatar(uid, selectedAvatarId)
            if (success) {
                Toast.makeText(this@ProfileActivity, "Avatar saved!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@ProfileActivity, "Failed to save profile.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupConfirmButton() {
        confirmButton.setOnClickListener {
            // Save locally first
            AvatarPreferenceManager.setUserAvatar(selectedAvatarId)

            val currentUser = auth.currentUser
            if (currentUser != null) {
                // User is logged in - save to Firestore and wait for completion
                lifecycleScope.launch {
                    val success = FirebaseManager.saveUserAvatar(currentUser.uid, selectedAvatarId)
                    if (success) {
                        Toast.makeText(this@ProfileActivity, "Avatar saved!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileActivity, "Failed to save avatar to cloud.", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }
            } else {
                // User is not logged in - only local save
                Toast.makeText(this, "Avatar saved locally!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
