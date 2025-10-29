package com.naijaayo.worldwide.sound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.naijaayo.worldwide.R
import com.naijaayo.worldwide.theme.NigerianThemeManager

class VolumeSettingsFragment : Fragment() {

    private lateinit var soundEnabledSwitch: Switch
    private lateinit var effectsVolumeSeekBar: SeekBar
    private lateinit var musicVolumeSeekBar: SeekBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_volume_settings, container, false)

        soundEnabledSwitch = view.findViewById(R.id.soundEnabledSwitch)
        effectsVolumeSeekBar = view.findViewById(R.id.volumeSeekBar)
        musicVolumeSeekBar = view.findViewById(R.id.backgroundVolumeSeekBar)

        setupEventListeners()
        loadSettings()

        return view
    }

    private fun setupEventListeners() {
        // Sound enabled switch
        soundEnabledSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            SoundPreferencesManager.setSoundEnabled(isChecked)
        }

        // Effects volume seek bar
        effectsVolumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    saveMasterVolume(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Not needed
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Not needed
            }
        })

        // Music volume seek bar
        musicVolumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    saveBackgroundVolume(progress)
                    BackgroundMusicManager.updateVolume()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Not needed
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Not needed
            }
        })
    }

    private fun loadSettings() {
        // Load sound enabled setting
        val soundEnabled = SoundPreferencesManager.isSoundEnabled()
        soundEnabledSwitch.isChecked = soundEnabled

        // Load effects volume setting
        effectsVolumeSeekBar.isEnabled = true
        val masterVolume = SoundPreferencesManager.getMasterVolumePercent()
        effectsVolumeSeekBar.progress = masterVolume

        // Load music volume setting
        musicVolumeSeekBar.isEnabled = true
        val backgroundVolume = SoundPreferencesManager.getBackgroundMusicVolumePercent()
        musicVolumeSeekBar.progress = backgroundVolume
    }

    private fun saveMasterVolume(volumePercent: Int) {
        SoundPreferencesManager.setMasterVolumePercent(volumePercent)
    }

    private fun saveBackgroundVolume(volumePercent: Int) {
        SoundPreferencesManager.setBackgroundMusicVolumePercent(volumePercent)
    }

    override fun onResume() {
        super.onResume()
        // Reapply theme
        NigerianThemeManager.applyThemeToActivity(requireActivity())
    }
}
