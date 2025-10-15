package com.naijaayo.worldwide.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.R
import com.naijaayo.worldwide.Room

class RoomAdapter(
    private var rooms: List<Room>,
    private val onRoomClicked: (Room) -> Unit
) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]
        holder.bind(room)
    }

    override fun getItemCount(): Int = rooms.size

    fun setData(newRooms: List<Room>) {
        this.rooms = newRooms
        notifyDataSetChanged()
    }

    inner class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val roomNameTextView: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(room: Room) {
            roomNameTextView.text = room.roomId
            itemView.setOnClickListener { onRoomClicked(room) }
        }
    }
}
