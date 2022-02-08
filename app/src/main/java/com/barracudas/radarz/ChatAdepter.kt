package com.barracudas.radarz

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.lang.Exception

class ChatAdepter(private val Friends: ArrayList<String>,
                  private val MyUID: String,
                  private val listener: FriendViewClicked): RecyclerView.Adapter<ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_for_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {

        var UserName: String = ""
        var ImageURL: String = ""

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(Friends[position]).get().addOnSuccessListener {
            UserName = it.data!!.getValue("Name").toString()
            ImageURL = it.data!!.getValue("DP").toString()
            holder.userName.text = UserName
            Picasso.get().load(ImageURL).into(holder.profileImage)
            holder.lastMessageTime.visibility = View.INVISIBLE
            holder.progressBars.visibility = View.INVISIBLE
            holder.userMessage.visibility = View.INVISIBLE
            holder.messageCount.visibility = View.INVISIBLE
        }

        holder.userName.setOnClickListener {
            listener.onFriendViewClicked(MyUID, Friends[position], ImageURL, UserName)
        }
        }

    override fun getItemCount(): Int {
        return Friends.size
    }

}

class ChatViewHolder(chats: View): RecyclerView.ViewHolder(chats) {
    val profileImage: ImageView = chats.findViewById(R.id.peopleImageInChatFrag)
    val progressBars: ProgressBar = chats.findViewById(R.id.progressBarInRecyclerViewChatFrag)
    val userName: TextView = chats.findViewById(R.id.PeopleNameInChatFrag)
    val userMessage: TextView = chats.findViewById(R.id.PeopleMessageInChatFrag)
    val lastMessageTime: TextView = chats.findViewById(R.id.DateAndTimeChatFrag)
    val messageCount: TextView = chats.findViewById(R.id.noOfChatsInChatFrag)
}

interface FriendViewClicked {
    fun onFriendViewClicked(myUID: String, friendUID: String, imageURL: String, userName: String)
}