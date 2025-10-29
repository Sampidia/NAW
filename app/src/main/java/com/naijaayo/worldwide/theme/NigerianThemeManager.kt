package com.naijaayo.worldwide.theme

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.view.ViewGroup
import androidx.core.content.ContextCompat

object NigerianThemeManager {

    private const val PREF_NAME = "nigerian_theme_prefs"
    private const val KEY_ACTIVE_THEME = "active_theme_id"

    private lateinit var sharedPreferences: SharedPreferences
    private var themes: List<NigerianTheme>? = null

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getAllThemes(): List<NigerianTheme> {
        if (themes == null) {
            themes = listOf(
                NigerianTheme(
                    id = "adire",
                    name = "adire",
                    displayName = "Adire Pattern",
                    backgroundImagePath = "adire_background.png",
                    isAvailable = true,
                    isActive = false,
                    category = ThemeCategory.ADIRE
                ),
                NigerianTheme(
                    id = "lagos",
                    name = "lagos",
                    displayName = "Lagos",
                    backgroundImagePath = "lagos_background.png",
                    isAvailable = true,
                    isActive = true, // Default active theme
                    category = ThemeCategory.LAGOS
                ),
                NigerianTheme(
                    id = "abuja",
                    name = "abuja",
                    displayName = "Abuja",
                    backgroundImagePath = "abuja_background.png",
                    isAvailable = true,
                    isActive = false,
                    category = ThemeCategory.ABUJA
                ),
                NigerianTheme(
                    id = "enugu",
                    name = "enugu",
                    displayName = "Enugu",
                    backgroundImagePath = "enugu_background.png",
                    isAvailable = true,
                    isActive = false,
                    category = ThemeCategory.ENUGU
                ),
                NigerianTheme(
                    id = "kano",
                    name = "kano",
                    displayName = "Kano",
                    backgroundImagePath = "kano_background.png",
                    isAvailable = true,
                    isActive = false,
                    category = ThemeCategory.KANO
                ),
                NigerianTheme(
                    id = "coming_soon",
                    name = "coming_soon",
                    displayName = "Coming Soon",
                    backgroundImagePath = "soon_background.png",
                    isAvailable = false,
                    isActive = false,
                    category = ThemeCategory.COMING_SOON
                )
            )
        }
        return themes!!
    }

    fun getActiveTheme(): NigerianTheme? {
        val activeThemeId = sharedPreferences.getString(KEY_ACTIVE_THEME, "lagos")
        return getAllThemes().find { it.id == activeThemeId } ?: getAllThemes().find { it.isActive }
    }

    fun setActiveTheme(themeId: String) {
        // Update shared preferences
        sharedPreferences.edit().putString(KEY_ACTIVE_THEME, themeId).apply()

        // Update in-memory theme list
        themes?.forEach { theme ->
            theme.isActive = theme.id == themeId
        }
    }

    fun applyThemeToActivity(activity: Activity) {
        val activeTheme = getActiveTheme()
        if (activeTheme != null && activeTheme.isAvailable) {
            // Apply theme background to activity's main layout
            try {
                val resourceName = activeTheme.backgroundImagePath.replace(".png", "")
                val resourceId = activity.resources.getIdentifier(
                    resourceName,
                    "drawable",
                    activity.packageName
                )

                if (resourceId != 0) {
                    val backgroundDrawable = ContextCompat.getDrawable(activity, resourceId)
                    if (backgroundDrawable != null) {
                        // Method 1: Try to set background on decor view (most reliable)
                        try {
                            activity.window.decorView.background = backgroundDrawable
                            android.util.Log.d("NigerianThemeManager", "Successfully applied theme via decorView: ${activeTheme.displayName}")
                            return
                        } catch (e: Exception) {
                            android.util.Log.w("NigerianThemeManager", "Failed to set background via decorView, trying alternative methods")
                        }

                        // Method 2: Try to find root ConstraintLayout
                        try {
                            val rootView = activity.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(
                                android.R.id.content
                            )?.getChildAt(0) as? androidx.constraintlayout.widget.ConstraintLayout
                            rootView?.background = backgroundDrawable
                            if (rootView != null) {
                                android.util.Log.d("NigerianThemeManager", "Successfully applied theme via root ConstraintLayout: ${activeTheme.displayName}")
                                return
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("NigerianThemeManager", "Failed to set background via root ConstraintLayout")
                        }

                        // Method 3: Try to find any root view
                        try {
                            val contentView = activity.findViewById<ViewGroup>(android.R.id.content)
                            if (contentView != null && contentView.childCount > 0) {
                                contentView.getChildAt(0).background = backgroundDrawable
                                android.util.Log.d("NigerianThemeManager", "Successfully applied theme via content view: ${activeTheme.displayName}")
                                return
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("NigerianThemeManager", "Failed to set background via content view")
                        }

                        android.util.Log.e("NigerianThemeManager", "All methods failed to apply theme: ${activeTheme.displayName}")
                    } else {
                        android.util.Log.e("NigerianThemeManager", "Failed to load drawable for resource: $resourceName")
                    }
                } else {
                    android.util.Log.e("NigerianThemeManager", "Resource not found: $resourceName (ID: $resourceId)")
                }
            } catch (e: Exception) {
                android.util.Log.e("NigerianThemeManager", "Error applying theme: ${e.message}", e)
            }
        } else {
            android.util.Log.d("NigerianThemeManager", "No active theme available or theme not available")
        }
    }

    fun saveThemePreference() {
        val activeTheme = getActiveTheme()
        if (activeTheme != null) {
            sharedPreferences.edit().putString(KEY_ACTIVE_THEME, activeTheme.id).apply()
        }
    }

    fun loadThemePreference(): String? {
        return sharedPreferences.getString(KEY_ACTIVE_THEME, "lagos")
    }
}
