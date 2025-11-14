package com.naijaayo.worldwide

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
// import com.naijaayo.worldwide.databinding.DialogResumeGameBinding

class ResumeGameDialog(
    private val context: Context,
    private val savedGames: List<SavedGame>,
    private val onGameSelected: (SavedGame) -> Unit,
    private val onStartNewGame: () -> Unit
) : Dialog(context) {

    // private lateinit var binding: DialogResumeGameBinding

    init {
        setupDialog()
    }

    private fun setupDialog() {
        // binding = DialogResumeGameBinding.inflate(LayoutInflater.from(context))
        // setContentView(binding.root)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_resume_game, null)
        setContentView(view)


        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )

        setupRecyclerView()
        setupButtons()
    }

    private fun setupRecyclerView() {
        val savedGamesRecyclerView = findViewById<RecyclerView>(R.id.savedGamesRecyclerView)
        val noSavedGamesText = findViewById<android.widget.TextView>(R.id.noSavedGamesText)

        if (savedGames.isEmpty()) {
            savedGamesRecyclerView.visibility = android.view.View.GONE
            noSavedGamesText.visibility = android.view.View.VISIBLE
        } else {
            savedGamesRecyclerView.visibility = android.view.View.VISIBLE
            noSavedGamesText.visibility = android.view.View.GONE

            val adapter = SavedGameAdapter(savedGames, onGameSelected)
            savedGamesRecyclerView.adapter = adapter
            savedGamesRecyclerView.layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupButtons() {
        val startNewGameButton = findViewById<android.widget.Button>(R.id.startNewGameButton)
        val cancelButton = findViewById<android.widget.Button>(R.id.cancelButton)

        startNewGameButton.setOnClickListener {
            onStartNewGame()
            dismiss()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }
}