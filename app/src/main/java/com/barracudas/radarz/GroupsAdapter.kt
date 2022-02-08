package com.barracudas.radarz

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso

class GroupsAdapter(private val item: ArrayList<String>,
                    private val lastMessage: ArrayList<String>,
                    private val lastTime: ArrayList<String>,
                    private val listener: GroupClicked,
                    private val listenerImage: ImageClicked): RecyclerView.Adapter<ViewHolderForGroupAdepter>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderForGroupAdepter {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.for_recycler_view_chat, parent, false)
        val viewHolder = ViewHolderForGroupAdepter(view)
        view.setOnClickListener {
            listener.onItemClicked(item[viewHolder.adapterPosition])
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolderForGroupAdepter, position: Int) {
        holder.groupName.text = item[position]

        val storageRef: StorageReference = FirebaseStorage.getInstance("gs://radaz-3b522.appspot.com").getReference()
        storageRef.child("Images/${item[position]}.jpg").downloadUrl
            .addOnSuccessListener {
                Log.d("Image", "Image downloaded")
                Picasso.get().load(it).into(holder.GroupImage)
                holder.progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                Log.d("Image", "Image failed $it")
                holder.progressBar.visibility = View.GONE
            }

        if (lastMessage[position] != null) {
            holder.LastMessage.text = lastMessage[position]
            holder.LastTime.text = lastTime[position]
        }

        holder.GroupImage.setOnClickListener {
            listenerImage.OnImageClicked(item[position], holder.GroupImage)
        }
    }

    override fun getItemCount(): Int {
         return item.size
    }

}

class ViewHolderForGroupAdepter(itemView: View) :RecyclerView.ViewHolder(itemView) {
    val groupName: TextView = itemView.findViewById(R.id.PeopleNameInChat)
    val LastMessage: TextView = itemView.findViewById(R.id.PeopleMessageInChat)
    val LastTime: TextView = itemView.findViewById(R.id.DateAndTime)
    val GroupImage: ImageView = itemView.findViewById(R.id.peopleImageInChat)
    val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarInRecyclerView)
}

interface GroupClicked {
    fun onItemClicked(item: String)
}

interface ImageClicked {
    fun OnImageClicked(item: String, image: ImageView)
}