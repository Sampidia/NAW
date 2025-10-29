package com.naijaayo.worldwide.ui

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.naijaayo.worldwide.R

/**
 * Manages visual seed display and animations for Ayo game pits
 */
class VisualSeedManager(private val context: Context) {

    /**
     * Animation types for different game events
     */
    enum class AnimationType {
        NONE,           // No animation
        CAPTURE,        // Pit capture effect
        SEED_ADDED,     // Seeds added effect
        SEED_REMOVED    // Seeds removed effect
    }

    /**
     * Updates visual seeds in a pit based on seed count
     */
    fun updatePitSeeds(pitImageView: ImageView, seedCount: Int) {
        updatePitSeeds(pitImageView, seedCount, AnimationType.NONE)
    }

    /**
     * Updates visual seeds with specific animation type
     */
    fun updatePitSeeds(pitImageView: ImageView, seedCount: Int, animationType: AnimationType) {
        // Set the appropriate seed image based on count
        val imageResource = getSeedImageResource(seedCount)
        pitImageView.setImageResource(imageResource)

        // Animate based on the specific event type
        when (animationType) {
            AnimationType.NONE -> {
                // No animation for routine updates
            }
            AnimationType.CAPTURE -> {
                animatePitCapture(pitImageView)
            }
            AnimationType.SEED_ADDED -> {
                animateSeedsAdded(pitImageView)
            }
            AnimationType.SEED_REMOVED -> {
                animateSeedsRemoved(pitImageView)
            }
        }
    }

    /**
     * Gets the appropriate drawable resource ID for a given seed count
     */
    private fun getSeedImageResource(seedCount: Int): Int {
        return when (seedCount) {
            0 -> R.drawable.zero_seed
            1 -> R.drawable.one_seed
            2 -> R.drawable.two_seed
            3 -> R.drawable.three_seed
            4 -> R.drawable.four_seed
            5 -> R.drawable.five_seed
            6 -> R.drawable.six_seed
            7 -> R.drawable.seven_seed
            8 -> R.drawable.eight_seed
            9 -> R.drawable.nine_seed
            10 -> R.drawable.ten_seed
            11 -> R.drawable.eleven_seed
            12 -> R.drawable.twelve_seed
            13 -> R.drawable.thirteen_seed
            14 -> R.drawable.fourteen_seed
            else -> R.drawable.fifteen_seed // 15 and above
        }
    }

    /**
     * Animates pit capture with golden glow effect
     */
    private fun animatePitCapture(imageView: ImageView) {
        // Golden glow effect for captured pits
        imageView.animate()
            .scaleX(1.3f)
            .scaleY(1.3f)
            .alpha(0.8f)
            .setDuration(200)
            .setListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    imageView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .alpha(1.0f)
                        .setDuration(200)
                        .setListener(null)
                }
            })
    }

    /**
     * Animates seeds being added with green pulse effect
     */
    private fun animateSeedsAdded(imageView: ImageView) {
        // Green pulse effect for added seeds
        imageView.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .alpha(0.9f)
            .setDuration(150)
            .setListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    imageView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .alpha(1.0f)
                        .setDuration(150)
                        .setListener(null)
                }
            })
    }

    /**
     * Animates seeds being removed with red fade effect
     */
    private fun animateSeedsRemoved(imageView: ImageView) {
        // Red fade effect for removed seeds
        imageView.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .alpha(0.7f)
            .setDuration(150)
            .setListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    imageView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .alpha(1.0f)
                        .setDuration(150)
                        .setListener(null)
                }
            })
    }

    /**
     * Legacy animation method for backward compatibility
     */
    private fun animateSeedImageChange(imageView: ImageView, newCount: Int) {
        // Simple scale and fade for routine updates
        imageView.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .alpha(0.8f)
            .setDuration(100)
            .setListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    imageView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .alpha(1.0f)
                        .setDuration(100)
                        .setListener(null)
                }
            })
    }

}
