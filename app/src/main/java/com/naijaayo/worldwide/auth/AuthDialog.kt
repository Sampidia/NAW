package com.naijaayo.worldwide.auth

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import com.naijaayo.worldwide.network.toUser
import com.google.android.material.tabs.TabLayout
import com.naijaayo.worldwide.R
import com.naijaayo.worldwide.network.RetrofitClient
import com.naijaayo.worldwide.network.RegisterRequest
import com.naijaayo.worldwide.network.LoginRequest
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
// import com.naijaayo.worldwide.databinding.DialogAuthBinding
import kotlinx.coroutines.launch

class AuthDialog(
    private val context: Context,
    private val onAuthSuccess: (String, String, String) -> Unit // userId, username, avatarId
) : Dialog(context) {

    // private lateinit var binding: DialogAuthBinding
    private var isSignUpMode = true

    init {
        setupDialog()
    }

    private fun setupDialog() {
        // binding = DialogAuthBinding.inflate(LayoutInflater.from(context))
        // setContentView(binding.root)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_auth, null)
        setContentView(view)

        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )

        setupTabs()
        setupButtons()
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

        if (isSignUpMode) {
            signUpLayout.visibility = android.view.View.VISIBLE
            signInLayout.visibility = android.view.View.GONE
            authActionButton.text = "Sign Up"
            dialogTitle.text = "Sign Up for Multiplayer"
        } else {
            signUpLayout.visibility = android.view.View.GONE
            signInLayout.visibility = android.view.View.VISIBLE
            authActionButton.text = "Sign In"
            dialogTitle.text = "Sign In to Multiplayer"
        }
        errorTextView.visibility = android.view.View.GONE
    }

    private fun setupButtons() {
        val authActionButton = findViewById<Button>(R.id.authActionButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)

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
                GlobalScope.launch(Dispatchers.Main) {
                    try {
                        val response = RetrofitClient.instance.register(RegisterRequest(username, email, password))
                        val user = response.user.toUser()
                        SessionManager.saveUserSession(user, response.token)
                        onAuthSuccess(user.id, user.username, user.avatarId)
                        dismiss()
                    } catch (e: Exception) {
                        val errorMessage = when (e) {
                            is retrofit2.HttpException -> {
                                e.response()?.errorBody()?.string() ?: "Registration failed"
                            }
                            else -> "Network error: ${e.message}"
                        }
                        showError(errorMessage)
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
                GlobalScope.launch(Dispatchers.Main) {
                    try {
                        val response = RetrofitClient.instance.login(LoginRequest(email, password))
                        val user = response.user.toUser()
                        SessionManager.saveUserSession(user, response.token)
                        onAuthSuccess(user.id, user.username, user.avatarId)
                        dismiss()
                    } catch (e: Exception) {
                        val errorMessage = when (e) {
                            is retrofit2.HttpException -> {
                                e.response()?.errorBody()?.string() ?: "Login failed"
                            }
                            else -> "Network error: ${e.message}"
                        }
                        showError(errorMessage)
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
