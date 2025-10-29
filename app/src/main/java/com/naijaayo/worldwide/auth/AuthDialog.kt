package com.naijaayo.worldwide.auth

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.naijaayo.worldwide.AuthRequest
import com.naijaayo.worldwide.AuthResponse
import com.naijaayo.worldwide.LoginRequest
import com.naijaayo.worldwide.R
import com.naijaayo.worldwide.databinding.DialogAuthBinding
import kotlinx.coroutines.launch

class AuthDialog(
    private val context: Context,
    private val onAuthSuccess: (String, String, String) -> Unit // userId, username, avatarId
) : Dialog(context) {

    private lateinit var binding: DialogAuthBinding
    private var isSignUpMode = true

    init {
        setupDialog()
    }

    private fun setupDialog() {
        binding = DialogAuthBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )

        setupTabs()
        setupButtons()
    }

    private fun setupTabs() {
        binding.authTabLayout.addTab(binding.authTabLayout.newTab().setText("Sign Up"))
        binding.authTabLayout.addTab(binding.authTabLayout.newTab().setText("Sign In"))

        binding.authTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                isSignUpMode = tab?.position == 0
                updateUI()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateUI() {
        if (isSignUpMode) {
            binding.signUpLayout.visibility = android.view.View.VISIBLE
            binding.signInLayout.visibility = android.view.View.GONE
            binding.authActionButton.text = "Sign Up"
            binding.dialogTitle.text = "Sign Up for Multiplayer"
        } else {
            binding.signUpLayout.visibility = android.view.View.GONE
            binding.signInLayout.visibility = android.view.View.VISIBLE
            binding.authActionButton.text = "Sign In"
            binding.dialogTitle.text = "Sign In to Multiplayer"
        }
        binding.errorTextView.visibility = android.view.View.GONE
    }

    private fun setupButtons() {
        binding.authActionButton.setOnClickListener {
            if (isSignUpMode) {
                performSignUp()
            } else {
                performSignIn()
            }
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun performSignUp() {
        val username = binding.usernameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()
        val confirmPassword = binding.confirmPasswordEditText.text.toString()

        // Validation
        when {
            username.isEmpty() -> showError("Username is required")
            email.isEmpty() -> showError("Email is required")
            password.isEmpty() -> showError("Password is required")
            password != confirmPassword -> showError("Passwords don't match")
            password.length < 6 -> showError("Password must be at least 6 characters")
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> showError("Invalid email format")
            else -> {
                // TODO: Call authentication service
                mockSignUp(username, email, password)
            }
        }
    }

    private fun performSignIn() {
        val email = binding.signInEmailEditText.text.toString().trim()
        val password = binding.signInPasswordEditText.text.toString()

        // Validation
        when {
            email.isEmpty() -> showError("Email is required")
            password.isEmpty() -> showError("Password is required")
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> showError("Invalid email format")
            else -> {
                // TODO: Call authentication service
                mockSignIn(email, password)
            }
        }
    }

    private fun mockSignUp(username: String, email: String, password: String) {
        // TODO: Replace with actual server call
        val userId = "user_${System.currentTimeMillis()}"
        val avatarId = "ayo" // Default avatar

        // Save session
        val user = com.naijaayo.worldwide.User(
            id = userId,
            username = username,
            email = email,
            avatarId = avatarId,
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()),
            isOnline = true
        )

        SessionManager.saveUserSession(user, "mock_jwt_token")

        onAuthSuccess(userId, username, avatarId)
        dismiss()
    }

    private fun mockSignIn(email: String, password: String) {
        // TODO: Replace with actual server call
        val userId = "user_${email.hashCode()}"
        val username = email.substringBefore("@")
        val avatarId = "ayo" // TODO: Get from server

        // Save session
        val user = com.naijaayo.worldwide.User(
            id = userId,
            username = username,
            email = email,
            avatarId = avatarId,
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()),
            isOnline = true
        )

        SessionManager.saveUserSession(user, "mock_jwt_token")

        onAuthSuccess(userId, username, avatarId)
        dismiss()
    }

    private fun showError(message: String) {
        binding.errorTextView.text = message
        binding.errorTextView.visibility = android.view.View.VISIBLE
    }
}