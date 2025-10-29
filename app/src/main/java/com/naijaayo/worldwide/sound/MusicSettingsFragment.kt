package com.naijaayo.worldwide.sound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.R
import com.naijaayo.worldwide.theme.NigerianThemeManager

class MusicSettingsFragment : Fragment() {

    private lateinit var musicRecyclerView: RecyclerView
    private lateinit var musicAdapter: MusicAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_music_settings, container, false)

        // Initialize BackgroundMusicManager
        BackgroundMusicManager.initialize(requireContext())

        musicRecyclerView = view.findViewById(R.id.musicRecyclerView)

        setupRecyclerView()

        // Start background music if not already playing
        if (!BackgroundMusicManager.isPlaying()) {
            BackgroundMusicManager.startBackgroundMusic()
        }

        return view
    }

    private fun setupRecyclerView() {
        val musicTracks = BackgroundMusicManager.getAllMusicTracks().toMutableList()

        // Set selected track from preferences
        val selectedTrackId = SoundPreferencesManager.getSelectedMusicTrack()
        val updatedTracks = musicTracks.map { track ->
            track.copy(isActive = (track.id == selectedTrackId))
        }

        musicAdapter = MusicAdapter(updatedTracks) { track ->
            onMusicTrackSelected(track)
        }

        musicRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        musicRecyclerView.adapter = musicAdapter
    }


    private fun onMusicTrackSelected(track: MusicTrack) {
        // Prevent unnecessary switching if already playing the same track
        if (BackgroundMusicManager.getCurrentTrack()?.id == track.id) {
            return
        }

        // Update selected track in adapter
        val updatedTracks = BackgroundMusicManager.getAllMusicTracks().map {
            it.copy(isActive = (it.id == track.id))
        }
        musicAdapter = MusicAdapter(updatedTracks) { selectedTrack ->
            onMusicTrackSelected(selectedTrack)
        }
        musicRecyclerView.adapter = musicAdapter

        // Save selection to preferences
        SoundPreferencesManager.setSelectedMusicTrack(track.id)

        // Switch to the selected track if track has audio
        if (track.hasAudio) {
            BackgroundMusicManager.switchTrack(track)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reapply theme
        NigerianThemeManager.applyThemeToActivity(requireActivity())
    }
}