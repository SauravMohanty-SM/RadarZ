package com.barracudas.radarz

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.scottyab.aescrypt.AESCrypt
import com.squareup.picasso.Picasso
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.EnumSet.of
import kotlin.collections.ArrayList

class ChatActivity : AppCompatActivity(), ChatMessage {

    private lateinit var mOwnUID: String
    private lateinit var mFriendUID: String
    private lateinit var mUserName: String
    private lateinit var mUserImageURL: String
    private lateinit var databaseDocument: String
    private lateinit var backBottom: ImageView
    private lateinit var UserName: TextView
    private lateinit var messageBox: EditText
    private lateinit var sendMessageBottom: ImageView
    private lateinit var DisplayPicture: ImageView
    private lateinit var mMessages: ArrayList<String>
    private lateinit var mTimes: ArrayList<String>
    private lateinit var mDate: ArrayList<String>
    private lateinit var mFriendUIDS: ArrayList<String>
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var DateBox: TextView


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        mOwnUID = intent.getStringExtra("ownUID").toString()
        mFriendUID = intent.getStringExtra("friendUID").toString()
        mUserName = intent.getStringExtra("UserName").toString()
        mUserImageURL = intent.getStringExtra("ImageUrl").toString()


        intiallize()

        UserName.text = mUserName

        if (mOwnUID > mFriendUID) {
            databaseDocument = mOwnUID+mFriendUID
        } else {
            databaseDocument = mFriendUID+mOwnUID
        }

        Picasso.get().load(mUserImageURL).into(DisplayPicture)

        loadDataFromFireStore()

        sendMessageBottom.setOnClickListener {
            sendDataToFireBase()
        }

        backBottom.setOnClickListener {
            this.finish()
        }

    }

    private fun intiallize() {
        backBottom = findViewById(R.id.backButtomViewChat)
        UserName = findViewById(R.id.UserNameInChat)
        messageBox = findViewById(R.id.sendingEditViewChat)
        sendMessageBottom = findViewById(R.id.sendingButtomMessageChat)
        DisplayPicture = findViewById(R.id.displayPicture)
        chatRecyclerView = findViewById(R.id.recyclerViewForChatInChat)
        DateBox = findViewById(R.id.ChatDateInChat)
        val layoutManager = LinearLayoutManager(this)
        chatRecyclerView.layoutManager = layoutManager
        layoutManager.stackFromEnd = true
        chatRecyclerView.getRecycledViewPool().setMaxRecycledViews(0,0)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendDataToFireBase() {
        val message = messageBox.text.toString()



        if (message != null) {

            sendMessageBottom.visibility = View.GONE

            val encryptedMessage = AESCrypt.encrypt(mOwnUID, message)

            val map = hashMapOf<Any, Any>(
                "from" to mOwnUID,
                "to" to mFriendUID,
                "text" to encryptedMessage,
                "createdAt" to com.google.firebase.Timestamp.now()
             )

            val db = FirebaseFirestore.getInstance()
            db.collection("chats").document(databaseDocument).collection("chat").document().set(map)
                .addOnSuccessListener {
                    Log.d("CHAT", "Success")
                    sendMessageBottom.visibility = View.VISIBLE
                    messageBox.setText("")
                }
                .addOnFailureListener {
                    Log.d("CHAT", "Failed $it")
                    sendMessageBottom.visibility = View.VISIBLE
                }
        } else {
            messageBox.error = "Enter a message"
        }
    }

    private fun loadDataFromFireStore() {
        mMessages = ArrayList<String>()
        mTimes = ArrayList<String>()
        mDate = ArrayList<String>()
        mFriendUIDS = ArrayList<String>()

        val db = FirebaseFirestore.getInstance()
        db.collection("chats").document(databaseDocument)
            .collection("chat").orderBy("createdAt")
            .addSnapshotListener { value, error ->
                if (value != null) {

                    mMessages.clear()
                    mTimes.clear()
                    mDate.clear()
                    mFriendUIDS.clear()

                    for (i in value.documents) {
                        var data = i.data?.getValue("createdAt") as com.google.firebase.Timestamp
                        var dateFormat =  SimpleDateFormat("MMM dd")
                        var date = dateFormat.format(Date(data.seconds*1000)).toString()
                        var timeFormat =  SimpleDateFormat("hh:mm a")
                        var time = timeFormat.format(Date(data.seconds*1000)).toString()
                        mMessages.add(i.data?.getValue("text").toString())
                        mTimes.add(time)
                        mDate.add(date)
                        mFriendUIDS.add(i.data?.getValue("from").toString())
                    }
                    chatRecyclerView.adapter = ChatMessageAdepter(mMessages, mTimes, mDate, mFriendUIDS, mOwnUID, mUserName, this)
                }
            }
    }

    override fun setDate(Dates: String) {
        DateBox.text = Dates
    }
}

