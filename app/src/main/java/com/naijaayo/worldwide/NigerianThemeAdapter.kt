package com.naijaayo.worldwide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.theme.NigerianTheme
import com.naijaayo.worldwide.theme.ThemeCategory

class NigerianThemeAdapter(
    private val themes: List<NigerianTheme>,
    private val onThemeClick: (NigerianTheme) -> Unit
) : RecyclerView.Adapter<NigerianThemeAdapter.ThemeViewHolder>() {

    private var selectedThemeId: String? = themes.find { it.isActive }?.id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.theme_grid_item, parent, false)
        return ThemeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        holder.bind(themes[position])
    }

    override fun getItemCount() = themes.size

    fun updateSelectedTheme(themeId: String) {
        selectedThemeId = themeId
        notifyDataSetChanged()
    }

    fun getSelectedThemeId(): String? = selectedThemeId

    inner class ThemeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val themeImage: ImageView = itemView.findViewById(R.id.themeImage)
        private val themeName: TextView = itemView.findViewById(R.id.themeName)
        private val activeIndicator: ImageView = itemView.findViewById(R.id.activeIndicator)
        private val comingSoonOverlay: TextView = itemView.findViewById(R.id.comingSoonOverlay)
        private val themeCard: androidx.cardview.widget.CardView = itemView.findViewById(R.id.themeCard)

        fun bind(theme: NigerianTheme) {
            // Set theme image
            val imageResId = getDrawableResourceId(theme.backgroundImagePath)
            if (imageResId != 0) {
                themeImage.setImageResource(imageResId)
            }

            // Set theme name
            themeName.text = theme.displayName

            // Show/hide active/selection indicator
            // Show indicator if theme is either:
            // 1. Currently active (saved in SharedPreferences), OR
            // 2. Currently selected (temporary selection before saving)
            val isActiveTheme = theme.isActive
            val isSelectedTheme = (selectedThemeId == theme.id)
            activeIndicator.visibility = if (isActiveTheme || isSelectedTheme) View.VISIBLE else View.GONE

            // Show/hide coming soon overlay
            if (!theme.isAvailable) {
                comingSoonOverlay.visibility = View.VISIBLE
                themeCard.isClickable = false
                themeCard.alpha = 0.5f
            } else {
                comingSoonOverlay.visibility = View.GONE
                themeCard.isClickable = true
                themeCard.alpha = 1.0f
            }

            // Set click listener
            itemView.setOnClickListener {
                if (theme.isAvailable) {
                    onThemeClick(theme)
                }
            }
        }

        private fun getDrawableResourceId(imagePath: String): Int {
            val imageName = imagePath.replace(".png", "")
            return itemView.context.resources.getIdentifier(
                imageName,
                "drawable",
                itemView.context.packageName
            )
        }
    }
}
