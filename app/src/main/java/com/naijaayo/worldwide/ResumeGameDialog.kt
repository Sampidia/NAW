package com.naijaayo.worldwide

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.databinding.DialogResumeGameBinding

class ResumeGameDialog(
    private val context: Context,
    private val savedGames: List<SavedGame>,
    private val onGameSelected: (SavedGame) -> Unit,
    private val onStartNewGame: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogResumeGameBinding

    init {
        setupDialog()
    }

    private fun setupDialog() {
        binding = DialogResumeGameBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )

        setupRecyclerView()
        setupButtons()
    }

    private fun setupRecyclerView() {
        if (savedGames.isEmpty()) {
            binding.savedGamesRecyclerView.visibility = android.view.View.GONE
            binding.noSavedGamesText.visibility = android.view.View.VISIBLE
        } else {
            binding.savedGamesRecyclerView.visibility = android.view.View.VISIBLE
            binding.noSavedGamesText.visibility = android.view.View.GONE

            val adapter = SavedGameAdapter(savedGames, onGameSelected)
            binding.savedGamesRecyclerView.adapter = adapter
            binding.savedGamesRecyclerView.layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupButtons() {
        binding.startNewGameButton.setOnClickListener {
            onStartNewGame()
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }
}