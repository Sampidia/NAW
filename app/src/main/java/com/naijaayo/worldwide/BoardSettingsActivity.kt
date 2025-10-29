package com.naijaayo.worldwide

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.theme.NigerianThemeManager

class BoardSettingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var boardAdapter: BoardAdapter
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize theme manager and apply current theme
        NigerianThemeManager.initialize(this)
        NigerianThemeManager.applyThemeToActivity(this)

        // Initialize board manager
        BoardManager.initialize(this)

        // Hide action bar to show only the logo image
        supportActionBar?.hide()

        setContentView(R.layout.activity_board_settings)

        // Initialize views
        recyclerView = findViewById(R.id.boardRecyclerView)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)

        // Setup RecyclerView with grid layout (2 columns)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        val boards = BoardManager.getAllBoards()
        boardAdapter = BoardAdapter(boards) { selectedBoard ->
            onBoardSelected(selectedBoard)
        }
        recyclerView.adapter = boardAdapter

        // Setup buttons
        saveButton.setOnClickListener {
            saveSelectedBoard()
        }

        cancelButton.setOnClickListener {
            // Reset selection to currently active board before canceling
            val activeBoard = BoardManager.getActiveBoard()
            if (activeBoard != null) {
                boardAdapter.updateSelectedBoard(activeBoard.id)
            }
            finish() // Go back without saving
        }
    }



    private fun onBoardSelected(board: BoardOption) {
        if (board.isAvailable) {
            // Update adapter to show selection
            boardAdapter.updateSelectedBoard(board.id)
        } else {
            Toast.makeText(this, "This board is coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSelectedBoard() {
        val selectedBoardId = boardAdapter.getSelectedBoardId()
        if (selectedBoardId != null) {
            BoardManager.setActiveBoard(selectedBoardId)
            BoardManager.saveBoardPreference()

            Toast.makeText(this, "Board saved successfully!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Please select a board first!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reapply theme when activity becomes visible
        NigerianThemeManager.applyThemeToActivity(this)
        // Resume background music
        com.naijaayo.worldwide.sound.BackgroundMusicManager.resumeBackgroundMusic()
    }
}
