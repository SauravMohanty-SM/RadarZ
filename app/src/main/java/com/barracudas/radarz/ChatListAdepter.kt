package com.barracudas.radarz

import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.scottyab.aescrypt.AESCrypt
import com.squareup.picasso.Picasso
import java.lang.Exception

class ChatListAdepter(private val item: ArrayList<Groups>,
                      private val uid: String,
                      private val listener: SetDate): RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        var view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_for_chat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val currentItem = item[position]

        downloadImage(holder, position, currentItem)

        var decryptedMessage = currentItem.Message

        try {
            decryptedMessage = AESCrypt.decrypt(currentItem.UserID, currentItem.Message)
        } catch (e: Exception) {

        }


        if (currentItem.UserID.equals(uid)) {
            holder.layout.gravity = Gravity.RIGHT
            holder.names.setTextColor(-0xffff01)

            holder.names.text = currentItem.Name
            holder.message.text = decryptedMessage
            holder.time.text = currentItem.Time
            listener.setDate(currentItem.Date!!)

        } else {
            holder.names.text = currentItem.Name
            holder.message.text = decryptedMessage
            holder.time.text = currentItem.Time
            listener.setDate(currentItem.Date!!)
        }
    }

    private fun downloadImage(holder: ViewHolder, position: Int, currentItem: Groups) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(currentItem.UserID.toString()).get()
            .addOnSuccessListener {
                Log.d("Chat", "Image is : ${it.data!!.get("DP")}")
                val image = it.data!!.get("DP")
                if (image != null) {
                    Picasso.get().load(image.toString()).into(holder.image)
                }
            }
            .addOnFailureListener {

            }
    }

    override fun getItemCount(): Int {
        return item.size
    }
}

class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

    var names: TextView = itemView.findViewById(R.id.NameInGroupChat)
    var message: TextView = itemView.findViewById(R.id.MessageInGroupChat)
    var time: TextView = itemView.findViewById(R.id.TimeInGroupChat)
    val layout: LinearLayout = itemView.findViewById(R.id.linearLayoutChatView)
    val image: ImageView = itemView.findViewById(R.id.ImageInGroupChat)
}

interface SetDate {
    fun setDate(date: String)
}