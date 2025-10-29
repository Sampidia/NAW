package com.naijaayo.worldwide.ui

import android.content.Context
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.naijaayo.worldwide.R

/**
 * Manages image loading and replacement for the Ayo game board and pits
 * Provides easy system for replacing PNG images
 */
class ImageManager(private val context: Context) {

    // Image resource IDs for easy replacement
    companion object {
        // Board images
        const val BOARD_BACKGROUND = "board_background"

        // Pit images
        const val PIT_NORMAL = "pit_normal"
        const val PIT_ACTIVE = "pit_active"
        const val PIT_HIGHLIGHT = "pit_highlight"

        // Game status images
        const val GAME_STATUS_BACKGROUND = "game_status_background"

        // Seed images
        const val SEED_1 = "seed_1"
        const val SEED_2 = "seed_2"
        const val SEED_3 = "seed_3"
        const val SEED_4 = "seed_4"
    }

    /**
     * Loads board background image with fallback
     */
    fun loadBoardBackground(imageView: ImageView, imageName: String = BOARD_BACKGROUND) {
        val resourceName = imageName.replace(".png", "")
        val resourceId = getDrawableResourceId(resourceName)
        if (resourceId != 0) {
            imageView.setImageResource(resourceId)
        } else {
            // Fallback to default wooden background
            try {
                imageView.setImageResource(R.drawable.wood_board_background)
            } catch (e: Exception) {
                // Final fallback to a simple color background if drawable fails
                imageView.setBackgroundColor(0xFF8B4513.toInt()) // Brown color
            }
        }
    }

    /**
     * Loads pit image with fallback
     */
    fun loadPitImage(imageView: ImageView, imageName: String = PIT_NORMAL) {
        val resourceId = getDrawableResourceId(imageName)
        if (resourceId != 0) {
            imageView.setImageResource(resourceId)
        } else {
            // Fallback to default pit shape
            try {
                imageView.setImageResource(R.drawable.wood_pit_shape)
            } catch (e: Exception) {
                // Final fallback to a simple oval shape color
                imageView.setBackgroundColor(0xFFDEB887.toInt()) // Burlywood color
            }
        }
    }

    /**
     * Loads active pit image with fallback
     */
    fun loadActivePitImage(imageView: ImageView, imageName: String = PIT_ACTIVE) {
        val resourceId = getDrawableResourceId(imageName)
        if (resourceId != 0) {
            imageView.setImageResource(resourceId)
        } else {
            // Fallback to default active pit
            try {
                imageView.setImageResource(R.drawable.wood_pit_active)
            } catch (e: Exception) {
                // Final fallback to a highlighted version of normal pit
                try {
                    imageView.setImageResource(R.drawable.wood_pit_shape)
                    imageView.setColorFilter(0xFFFFD700.toInt()) // Gold highlight
                } catch (e2: Exception) {
                    imageView.setBackgroundColor(0xFFCD853F.toInt()) // Peru color
                }
            }
        }
    }

    /**
     * Updates pit appearance based on game state
     */
    fun updatePitAppearance(imageView: ImageView, isActive: Boolean, seedCount: Int) {
        when {
            isActive -> loadActivePitImage(imageView)
            seedCount > 0 -> loadPitImage(imageView)
            else -> loadPitImage(imageView) // Empty pit
        }
    }

    /**
     * Gets drawable resource ID by name
     */
    private fun getDrawableResourceId(imageName: String): Int {
        return context.resources.getIdentifier(imageName, "drawable", context.packageName)
    }

    /**
     * Checks if a custom image exists
     */
    fun hasCustomImage(imageName: String): Boolean {
        return getDrawableResourceId(imageName) != 0
    }

    /**
     * Loads game status background image with fallback
     */
    fun loadGameStatusBackground(imageView: ImageView, imageName: String = GAME_STATUS_BACKGROUND) {
        val resourceId = getDrawableResourceId(imageName)
        if (resourceId != 0) {
            imageView.setImageResource(resourceId)
        } else {
            // Fallback to default game status background
            try {
                imageView.setImageResource(R.drawable.wood_board_background)
            } catch (e: Exception) {
                // Final fallback to a simple dark background
                imageView.setBackgroundColor(0xFF2F2F2F.toInt()) // Dark gray
            }
        }
    }

    /**
     * Gets available image options for replacement
     */
    fun getImageReplacementGuide(): Map<String, String> {
        return mapOf(
            BOARD_BACKGROUND to "Place your wooden board image as 'drawable/board_background.png'",
            PIT_NORMAL to "Place your normal pit image as 'drawable/pit_normal.png'",
            PIT_ACTIVE to "Place your active pit image as 'drawable/pit_active.png'",
            GAME_STATUS_BACKGROUND to "Place your game status background as 'drawable/game_status_background.png'"
        )
    }
}
