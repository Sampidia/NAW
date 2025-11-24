package com.naijaayo.worldwide.auth

import com.google.firebase.auth.FirebaseAuth
import com.naijaayo.worldwide.model.User

object SessionManager {
    fun getCurrentUser(): User? {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        return firebaseUser?.let {
            User(id = it.uid, email = it.email ?: "", username = it.displayName ?: "")
        }
    }
}
