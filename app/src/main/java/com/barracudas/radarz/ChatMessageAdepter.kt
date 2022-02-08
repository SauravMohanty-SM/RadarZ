package com.barracudas.radarz

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.scottyab.aescrypt.AESCrypt

class ChatMessageAdepter(private val messages: ArrayList<String>,
                         private val time: ArrayList<String>,
                         private val date: ArrayList<String>,
                         private val friendUID: ArrayList<String>,
                         private val OwnUID: String,
                         private val FriendName: String,
                         private val listener: ChatMessage): RecyclerView.Adapter<MessageChatViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageChatViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_for_chat, parent, false)
        return MessageChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageChatViewHolder, position: Int) {
        holder.image.visibility = View.GONE

        var decryptedMessage: String = try {
            AESCrypt.decrypt(friendUID[position], messages[position])
        } catch (e: Exception) {
            messages[position]
        }

        if (friendUID[position] == OwnUID) {
            holder.layouts.gravity = Gravity.RIGHT
            holder.names.setTextColor(-0xffff01)
            holder.names.text = "You"
            holder.message.text = decryptedMessage
            holder.time.text = time[position]
        } else {
            holder.message.text = decryptedMessage
            holder.time.text = time[position]
            holder.names.text = FriendName
        }

        listener.setDate(date[position])
    }

    override fun getItemCount(): Int {
       return messages.size
    }

}

class MessageChatViewHolder(item: View): RecyclerView.ViewHolder(item) {
    var names: TextView = item.findViewById(R.id.NameInGroupChat)
    var message: TextView = item.findViewById(R.id.MessageInGroupChat)
    var time: TextView = item.findViewById(R.id.TimeInGroupChat)
    val layouts: LinearLayout = item.findViewById(R.id.linearLayoutChatView)
    val image: ImageView = item.findViewById(R.id.ImageInGroupChat)
}

interface ChatMessage {
    fun setDate(Dates: String)
}