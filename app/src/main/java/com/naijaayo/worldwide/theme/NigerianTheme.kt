package com.naijaayo.worldwide.theme

data class NigerianTheme(
    val id: String,
    val name: String,
    val displayName: String,
    val backgroundImagePath: String,
    val isAvailable: Boolean = true,
    var isActive: Boolean = false,
    val category: ThemeCategory
)

enum class ThemeCategory(val displayName: String) {
    ADIRE("Adire Pattern"),
    LAGOS("Lagos"),
    ABUJA("Abuja"),
    ENUGU("Enugu"),
    KANO("Kano"),
    COMING_SOON("Coming Soon")
}
