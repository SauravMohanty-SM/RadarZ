package com.barracudas.radarz

import android.app.ProgressDialog
import android.content.Intent.ACTION_SEARCH
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.actions.SearchIntents.ACTION_SEARCH
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.lang.Exception

class SearchActivity : AppCompatActivity(), AddBottomClicked {

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mSearchBar: EditText
    private lateinit var mClassName: String
    private lateinit var mImageUrl: ArrayList<String>
    private lateinit var mStatus: ArrayList<String>
    private lateinit var mName: ArrayList<String>
    private lateinit var mUID: ArrayList<String>
    private lateinit var mCurrentUser: FirebaseUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        mClassName = intent.getStringExtra("Class").toString()

        initiallize()

        mSearchBar.setOnEditorActionListener { v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_SEARCH){
                if (mClassName == "Chat") {
                    receiveDataFromFireStore()
                } else if (mClassName == "Group") {
                    receiveDataFromRealTimeDataBase()
                }
                true
            } else {
                false
            }
        }

    }

    private fun initiallize(){
        mSearchBar = findViewById(R.id.SearchBar)
        mRecyclerView = findViewById(R.id.recyclerViewSearch)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mCurrentUser = Firebase.auth.currentUser!!
    }

    private fun receiveDataFromFireStore() {

        var name = mSearchBar.text.toString()
        var UserName: String

        var adepter: MapAdepter

        mImageUrl = ArrayList<String>()
        mStatus = ArrayList<String>()
        mName = ArrayList<String>()
        mUID = ArrayList<String>()

        var FriendUID: String

        val db = FirebaseFirestore.getInstance()
        db.collection("users").get()
            .addOnSuccessListener {
                for (document in it.documents) {
                    Log.d("MAP", "The Map are : $it")

                    FriendUID = document.data?.getValue("Uid").toString()

                     UserName = document.data?.getValue("Name").toString().lowercase()

                    if (UserName.contains(name)) {
                        mImageUrl.add(document.data?.getValue("DP").toString())
                        mStatus.add(document.data?.getValue("Status").toString())
                        mName.add(document.data?.getValue("Name").toString())
                        mUID.add(document.data?.getValue("Uid").toString())
                    }
                }
                adepter = MapAdepter(mImageUrl, mStatus, mName, mUID, mCurrentUser.uid ,this)
                mRecyclerView.adapter = adepter
            }
    }
    private fun receiveDataFromRealTimeDataBase() {
        Toast.makeText(this, "Realtime Database", Toast.LENGTH_SHORT).show()
    }

    override fun whenAddBottomClicked(friendUID: String, Image: ImageView) {
        val progressBar = ProgressDialog(this)
        progressBar.setTitle("Adding Friend")
        progressBar.setMessage("Please wait while we adding to your friend")
        progressBar.setCancelable(false)
        progressBar.show()

        val map = hashMapOf(
            "createdAt" to "",
            "from" to mCurrentUser.uid,
            "text" to "",
            "to" to friendUID
        )

        if (friendUID != null && mCurrentUser.uid != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(mCurrentUser.uid).update("FriendList", FieldValue.arrayUnion(friendUID))
                .addOnSuccessListener {
                    db.collection("users").document(friendUID).
                    update("FriendList", FieldValue.arrayUnion(mCurrentUser.uid))
                        .addOnSuccessListener {
                            progressBar.dismiss()
                            Toast.makeText(this, "Added to your friend", Toast.LENGTH_SHORT).show()
                            Image.visibility = View.GONE
                        }
                        .addOnFailureListener {
                            progressBar.dismiss()
                            Toast.makeText(this, "Failed to add friend", Toast.LENGTH_SHORT).show()
                        }
                }
        }
    }
}