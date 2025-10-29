package com.naijaayo.worldwide

data class BoardOption(
    val id: String,
    val displayName: String,
    val backgroundImagePath: String,
    val isAvailable: Boolean = true,
    var isActive: Boolean = false
)
