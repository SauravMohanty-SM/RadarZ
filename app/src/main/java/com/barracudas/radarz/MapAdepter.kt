package com.barracudas.radarz

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.SupportMapFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.lang.Exception

class MapAdepter(private val mImage: ArrayList<String>,
                 private val mStatus: ArrayList<String>,
                 private val mName: ArrayList<String>,
                 private val mUid: ArrayList<String>,
                 private val MyUID: String,
                 private val addBottomClicked: AddBottomClicked): RecyclerView.Adapter<ViewHolderMap>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderMap {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_for_map, parent, false)
        return ViewHolderMap(view)
    }

    override fun onBindViewHolder(holder: ViewHolderMap, position: Int) {

        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(MyUID).get().addOnSuccessListener {
                try {
                    val arrayList: ArrayList<String> = it.data!!.getValue("FriendList") as ArrayList<String>
                    for (i in arrayList) {
                        if (i == mUid[position]) {
                            holder.add.visibility = View.GONE
                        }
                    }
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {

        }



        holder.names.text = mName[position]
        holder.status.text = mStatus[position]
        try {
            Picasso.get().load(mImage[position]).into(holder.image)
            holder.progressBar.visibility = View.GONE
        } catch (e: Exception) {
            holder.progressBar.visibility = View.GONE
        }

        holder.add.setOnClickListener {
            addBottomClicked.whenAddBottomClicked(mUid[position], holder.add)
        }


    }

    override fun getItemCount(): Int {
        return mImage.size
    }

}

class ViewHolderMap(item: View): RecyclerView.ViewHolder(item) {
    val image: ImageView = item.findViewById(R.id.peopleImageInMap)
    val progressBar: ProgressBar = item.findViewById(R.id.progressBarInRecyclerViewMap)
    val names: TextView = item.findViewById(R.id.PeopleNameInMap)
    val status: TextView = item.findViewById(R.id.PeopleStatusInMap)
    val add: ImageView = item.findViewById(R.id.AddBottomForFriendMap)
}

interface AddBottomClicked {
    fun whenAddBottomClicked(friendUID: String, Image: ImageView)
}