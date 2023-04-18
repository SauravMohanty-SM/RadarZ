package com.barracudas.radarz

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.scottyab.aescrypt.AESCrypt
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MessaginActivity : AppCompatActivity() {

    lateinit var GroupName: String
    private lateinit var GroupNameTextView: TextView
    private lateinit var SendBottom: ImageView
    private lateinit var BackBottom: ImageView
    private lateinit var MessageArea: EditText
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var key: String
    private lateinit var UserName: String
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var chatArrayList: ArrayList<Groups>
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var Date: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messagin)

        initillize()

        GroupName = intent.getStringExtra("GroupName").toString()
        GroupNameTextView.text = GroupName

        receiveNameFromFireStore()

        retrieveDataFromFirebase()


        SendBottom.setOnClickListener {
            sendDataToFirebase()
        }

        BackBottom.setOnClickListener {
            finish()
        }

    }


    private fun initillize() {
        GroupNameTextView = findViewById(R.id.UserNameInGroup)
        SendBottom = findViewById(R.id.sendingButtomMessage)
        MessageArea = findViewById(R.id.sendingEditView)
        BackBottom = findViewById(R.id.backButtomView)
        chatRecyclerView = findViewById(R.id.recyclerViewForChat)
        Date = findViewById(R.id.ChatDate)

        database = FirebaseDatabase.getInstance()
        reference = database.getReference("Groups")
        firebaseAuth = FirebaseAuth.getInstance()
        currentUser = firebaseAuth.currentUser!!
        chatArrayList = arrayListOf<Groups>()
        val layoutManager = LinearLayoutManager(this)
        chatRecyclerView.layoutManager = layoutManager
        layoutManager.stackFromEnd = true
        chatRecyclerView.recycledViewPool.setMaxRecycledViews(0, 0)
    }


    private fun sendDataToFirebase() {
        var message = MessageArea.text.toString()

        if (message != null) {

            key = reference.child(GroupName).push().key.toString()

            val dateFormat = SimpleDateFormat("MMM dd")
            val currentDate = dateFormat.format(Date())

            val timeFormat = SimpleDateFormat("hh:mm a")
            val currentTime = timeFormat.format(Date())

            Log.d("LOG", "Date is $currentDate and Time is $currentTime")
            Log.d("LOG", "The user uid is ${currentUser.uid}")

            val encryptedMessage = AESCrypt.encrypt(currentUser.uid, message)
            //TODO: Map for Message
            var map: HashMap<String, Any> = HashMap<String, Any>()
            map.put("Message", encryptedMessage)
            map.put("Date", currentDate)
            map.put("Time", currentTime)
            map.put("UserID", currentUser.uid)
            map.put("Name", UserName)

            //TODO: Map for LastDetails
            var lastDetails: HashMap<String, String> = HashMap()
            lastDetails.put("Message", message)
            lastDetails.put("Date", currentDate)
            lastDetails.put("Time", currentTime)
            lastDetails.put("UserID", currentUser.uid)
            lastDetails.put("Name", UserName)

            SendBottom.visibility = View.GONE

            reference.child(GroupName).child("LastDetails").setValue(lastDetails)

            reference.child(GroupName).child(key).setValue(map)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        SendBottom.visibility = View.VISIBLE
                        MessageArea.setText("")
                    } else {
                        Toast.makeText(
                            this,
                            "Unable to connect. Please Try again",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }


        } else {
            Toast.makeText(this, "Enter Text First", Toast.LENGTH_SHORT).show()
        }
    }

    private fun retrieveDataFromFirebase() {

        val firebaseReference =
            FirebaseDatabase.getInstance().reference.child("Groups").child(GroupName)


        val postListener = object : ValueEventListener, SetDate {
            override fun onDataChange(snapshot: DataSnapshot) {

                chatArrayList.clear()
                Log.d("LOG", "The message is : $snapshot")
                if (snapshot.exists()) {
                    for (groupSnapshot in snapshot.children) {
                        if (groupSnapshot.key != "LastDetails") {
                            val groupMessage = groupSnapshot.getValue(Groups::class.java)
                            if (groupMessage!!.Message != null) {
                                chatArrayList.add(groupMessage!!)
                            }
                        }
                    }
                }

                chatRecyclerView.adapter = ChatListAdepter(chatArrayList, currentUser.uid, this)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MessaginActivity, "Unable to fetch message", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun setDate(date: String) {
                Date.text = date
            }

        }

        firebaseReference.addValueEventListener(postListener)
    }

    private fun receiveNameFromFireStore() {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener {
                UserName = it.get("Name").toString()
            }
    }

    override fun onStart() {
        super.onStart()
        val firebaseAuth: FirebaseAuth = Firebase.auth
        val CurrentUser = firebaseAuth.currentUser
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(CurrentUser!!.uid).update(mapOf("IsActive" to true))
            .addOnSuccessListener {

            }
    }
}