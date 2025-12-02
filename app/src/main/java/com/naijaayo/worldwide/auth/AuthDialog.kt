package com.naijaayo.worldwide.auth

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.naijaayo.worldwide.R
import com.naijaayo.worldwide.network.FirebaseManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.ImageView
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class AuthDialog(
    private val context: Context,
    private val onAuthSuccess: (String, String, String) -> Unit // userId, username, avatarId
) : Dialog(context) {

    private var isSignUpMode = true

    init {
        setupDialog()
    }

    private fun setupDialog() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_auth, null)
        setContentView(view)

        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )

        // Load GIF with Glide
        val logoImageView = findViewById<ImageView>(R.id.logoImageView)
        Glide.with(context).load(R.raw.logo_animate).into(logoImageView)

        setupTabs()
        setupButtons()
        setupHoverAnimations()
    }

    private fun setupTabs() {
        val authTabLayout = findViewById<TabLayout>(R.id.authTabLayout)
        authTabLayout.addTab(authTabLayout.newTab().setText("Sign Up"))
        authTabLayout.addTab(authTabLayout.newTab().setText("Sign In"))

        authTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                isSignUpMode = tab?.position == 0
                updateUI()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateUI() {
        val signUpLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.signUpLayout)
        val signInLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.signInLayout)
        val authActionButton = findViewById<Button>(R.id.authActionButton)
        val dialogTitle = findViewById<TextView>(R.id.dialogTitle)
        val errorTextView = findViewById<TextView>(R.id.errorTextView)
        val switchModeTextView = findViewById<TextView>(R.id.switchModeTextView)

        if (isSignUpMode) {
            signUpLayout.visibility = android.view.View.VISIBLE
            signInLayout.visibility = android.view.View.GONE
            authActionButton.text = "Sign Up"
            dialogTitle.text = "Sign Up for Multiplayer"
            switchModeTextView.text = "Already have an account? Sign in"
        } else {
            signUpLayout.visibility = android.view.View.GONE
            signInLayout.visibility = android.view.View.VISIBLE
            authActionButton.text = "Sign In"
            dialogTitle.text = "Sign In to Multiplayer"
            switchModeTextView.text = "Don't have an account? Sign up"
        }
        errorTextView.visibility = android.view.View.GONE
    }

    private fun setupButtons() {
        val authActionButton = findViewById<Button>(R.id.authActionButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val forgetPasswordTextView = findViewById<TextView>(R.id.forgetPasswordTextView)
        val switchModeTextView = findViewById<TextView>(R.id.switchModeTextView)

        authActionButton.setOnClickListener {
            if (isSignUpMode) {
                performSignUp()
            } else {
                performSignIn()
            }
        }

        cancelButton.setOnClickListener {
            dismiss()
        }

        forgetPasswordTextView.setOnClickListener {
            val email = findViewById<EditText>(R.id.signInEmailEditText).text.toString().trim()
            if (email.isEmpty()) {
                showError("Please enter your email in the Sign In field first")
            } else {
                (context as androidx.appcompat.app.AppCompatActivity).lifecycleScope.launch {
                    val success = FirebaseManager.resetPassword(email)
                    if (success) {
                        Toast.makeText(context, "Password reset email sent to $email", Toast.LENGTH_LONG).show()
                    } else {
                        showError("Failed to send reset email")
                    }
                }
            }
        }

        switchModeTextView.setOnClickListener {
            val authTabLayout = findViewById<TabLayout>(R.id.authTabLayout)
            val newTabPosition = if (isSignUpMode) 1 else 0
            authTabLayout.getTabAt(newTabPosition)?.select()
        }
    }

    private fun setupHoverAnimations() {
        val forgetPasswordTextView = findViewById<TextView>(R.id.forgetPasswordTextView)
        val switchModeTextView = findViewById<TextView>(R.id.switchModeTextView)

        addHoverAnimation(forgetPasswordTextView)
        addHoverAnimation(switchModeTextView)
    }

    private fun addHoverAnimation(view: android.view.View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start()
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }
            }
            false // Let the click listener handle the actual click
        }
    }

    private fun performSignUp() {
        val username = findViewById<EditText>(R.id.usernameEditText).text.toString().trim()
        val email = findViewById<EditText>(R.id.emailEditText).text.toString().trim()
        val password = findViewById<EditText>(R.id.passwordEditText).text.toString()
        val confirmPassword = findViewById<EditText>(R.id.confirmPasswordEditText).text.toString()

        // Validation
        when {
            username.isEmpty() -> showError("Username is required")
            email.isEmpty() -> showError("Email is required")
            password.isEmpty() -> showError("Password is required")
            password != confirmPassword -> showError("Passwords don't match")
            password.length < 6 -> showError("Password must be at least 6 characters")
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> showError("Invalid email format")
            else -> {
                (context as androidx.appcompat.app.AppCompatActivity).lifecycleScope.launch {
                    val success = FirebaseManager.registerUser(email, password, username)
                    if (success) {
                        val user = FirebaseManager.auth.currentUser!!
                        onAuthSuccess(user.uid, username, "ayo") // Assume default avatar
                        dismiss()
                    } else {
                        showError("Registration failed")
                    }
                }
            }
        }
    }

    private fun performSignIn() {
        val email = findViewById<EditText>(R.id.signInEmailEditText).text.toString().trim()
        val password = findViewById<EditText>(R.id.signInPasswordEditText).text.toString()

        // Validation
        when {
            email.isEmpty() -> showError("Email is required")
            password.isEmpty() -> showError("Password is required")
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> showError("Invalid email format")
            else -> {
                (context as androidx.appcompat.app.AppCompatActivity).lifecycleScope.launch {
                    val success = FirebaseManager.loginUser(email, password)
                    if (success) {
                        val user = FirebaseManager.auth.currentUser!!
                        val userProfile = FirebaseManager.getUserProfile(user.uid)
                        val username = userProfile?.get("displayName") as? String ?: "Player"
                        val avatarId = userProfile?.get("avatarId") as? String ?: "ayo"
                        onAuthSuccess(user.uid, username, avatarId)
                        dismiss()
                    } else {
                        showError("Login failed")
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        val errorTextView = findViewById<TextView>(R.id.errorTextView)
        errorTextView.text = message
        errorTextView.visibility = android.view.View.VISIBLE
    }
}
