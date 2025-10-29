package com.naijaayo.worldwide.sound

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.R

class MusicAdapter(
    private val musicTracks: List<MusicTrack>,
    private val onMusicClick: (MusicTrack) -> Unit
) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    private var selectedTrackId: String? = musicTracks.find { it.isActive }?.id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.music_grid_item, parent, false)
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        holder.bind(musicTracks[position])
    }

    override fun getItemCount() = musicTracks.size

    fun updateSelectedTrack(trackId: String) {
        selectedTrackId = trackId
        notifyDataSetChanged()
    }

    fun getSelectedTrackId(): String? = selectedTrackId

    inner class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val musicImage: ImageView = itemView.findViewById(R.id.musicImage)
        private val musicName: TextView = itemView.findViewById(R.id.musicName)
        private val activeIndicator: ImageView = itemView.findViewById(R.id.activeIndicator)
        private val comingSoonOverlay: TextView = itemView.findViewById(R.id.comingSoonOverlay)
        private val musicCard: androidx.cardview.widget.CardView = itemView.findViewById(R.id.musicCard)

        fun bind(track: MusicTrack) {
            // Set music image
            val imageResId = getDrawableResourceId(track.drawableResName)
            if (imageResId != 0) {
                musicImage.setImageResource(imageResId)
            }

            // Set music name
            musicName.text = track.displayName

            // Show/hide active/selection indicator
            val isActiveTrack = track.isActive
            val isSelectedTrack = (selectedTrackId == track.id)
            activeIndicator.visibility = if (isActiveTrack || isSelectedTrack) View.VISIBLE else View.GONE

            // Show/hide coming soon overlay
            if (!track.isAvailable) {
                comingSoonOverlay.visibility = View.VISIBLE
                musicCard.isClickable = false
                musicCard.alpha = 0.5f
            } else {
                comingSoonOverlay.visibility = View.GONE
                musicCard.isClickable = true
                musicCard.alpha = 1.0f
            }

            // Set click listener
            itemView.setOnClickListener {
                if (track.isAvailable) {
                    onMusicClick(track)
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