package com.naijaayo.worldwide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class BoardAdapter(
    private val boards: List<BoardOption>,
    private val onBoardClick: (BoardOption) -> Unit
) : RecyclerView.Adapter<BoardAdapter.BoardViewHolder>() {

    private var selectedBoardId: String? = boards.find { it.isActive }?.id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.board_grid_item, parent, false)
        return BoardViewHolder(view)
    }

    override fun onBindViewHolder(holder: BoardViewHolder, position: Int) {
        holder.bind(boards[position])
    }

    override fun getItemCount() = boards.size

    fun updateSelectedBoard(boardId: String) {
        selectedBoardId = boardId
        notifyDataSetChanged()
    }

    fun getSelectedBoardId(): String? = selectedBoardId

    inner class BoardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val boardImage: ImageView = itemView.findViewById(R.id.boardImage)
        private val boardName: TextView = itemView.findViewById(R.id.boardName)
        private val activeIndicator: ImageView = itemView.findViewById(R.id.activeIndicator)
        private val comingSoonOverlay: TextView = itemView.findViewById(R.id.comingSoonOverlay)
        private val boardCard: androidx.cardview.widget.CardView = itemView.findViewById(R.id.boardCard)

        fun bind(board: BoardOption) {
            // Set board image
            val imageResId = getDrawableResourceId(board.backgroundImagePath)
            if (imageResId != 0) {
                boardImage.setImageResource(imageResId)
            }

            // Set board name
            boardName.text = board.displayName

            // Show/hide active/selection indicator
            val isActiveBoard = board.isActive
            val isSelectedBoard = (selectedBoardId == board.id)
            activeIndicator.visibility = if (isActiveBoard || isSelectedBoard) View.VISIBLE else View.GONE

            // Show/hide coming soon overlay
            if (!board.isAvailable) {
                comingSoonOverlay.visibility = View.VISIBLE
                boardCard.isClickable = false
                boardCard.alpha = 0.5f
            } else {
                comingSoonOverlay.visibility = View.GONE
                boardCard.isClickable = true
                boardCard.alpha = 1.0f
            }

            // Set click listener
            itemView.setOnClickListener {
                if (board.isAvailable) {
                    onBoardClick(board)
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
