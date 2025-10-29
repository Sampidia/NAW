package com.naijaayo.worldwide

import android.content.Context
import android.content.SharedPreferences

object BoardManager {

    private const val PREF_NAME = "board_prefs"
    private const val KEY_ACTIVE_BOARD = "active_board_id"

    private lateinit var sharedPreferences: SharedPreferences
    private var boardOptions: List<BoardOption>? = null

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getAllBoards(): List<BoardOption> {
        if (boardOptions == null) {
            boardOptions = listOf(
                BoardOption(
                    id = "board1",
                    displayName = "Board 1",
                    backgroundImagePath = "board_background.png",
                    isAvailable = true,
                    isActive = true // Default active board
                ),
                BoardOption(
                    id = "board2",
                    displayName = "Board 2",
                    backgroundImagePath = "brown_board.png",
                    isAvailable = true,
                    isActive = false
                ),
                BoardOption(
                    id = "board3",
                    displayName = "Board 3",
                    backgroundImagePath = "dark_board.png",
                    isAvailable = true,
                    isActive = false
                ),
                BoardOption(
                    id = "coming_soon",
                    displayName = "Coming Soon",
                    backgroundImagePath = "soon_background.png",
                    isAvailable = false,
                    isActive = false
                )
            )
        }
        return boardOptions!!
    }

    fun getActiveBoard(): BoardOption? {
        val activeBoardId = sharedPreferences.getString(KEY_ACTIVE_BOARD, "board1")
        return getAllBoards().find { it.id == activeBoardId } ?: getAllBoards().find { it.isActive }
    }

    fun setActiveBoard(boardId: String) {
        // Update shared preferences
        sharedPreferences.edit().putString(KEY_ACTIVE_BOARD, boardId).apply()

        // Update in-memory board list
        boardOptions?.forEach { board ->
            board.isActive = board.id == boardId
        }
    }

    fun saveBoardPreference() {
        val activeBoard = getActiveBoard()
        if (activeBoard != null) {
            sharedPreferences.edit().putString(KEY_ACTIVE_BOARD, activeBoard.id).apply()
        }
    }

    fun loadBoardPreference(): String? {
        return sharedPreferences.getString(KEY_ACTIVE_BOARD, "board1")
    }

    fun getBoardImagePath(boardId: String): String? {
        return getAllBoards().find { it.id == boardId }?.backgroundImagePath
    }
}
