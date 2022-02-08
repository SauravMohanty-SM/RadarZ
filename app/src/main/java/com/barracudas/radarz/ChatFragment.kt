package com.barracudas.radarz

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.lang.Exception

class ChatFragment : Fragment(), FriendViewClicked {
    // TODO: Rename and change types of parameters
    private lateinit var auth: FirebaseAuth
    private lateinit var logOut : ImageView
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mCurrentUser: FirebaseUser
    private lateinit var mFriendList: ArrayList<String>
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initiallization(view)
        logOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(context, LoginActivity::class.java))
            activity?.finish()
        }

        getDataFromFireStore(view)
    }

    private fun initiallization(view: View) {
        auth = Firebase.auth
        logOut = view.findViewById(R.id.LogoutBottomChat)
        mAuth = Firebase.auth
        mCurrentUser = mAuth.currentUser!!
        recyclerView = view.findViewById(R.id.recycleViewChat)
        recyclerView.layoutManager = LinearLayoutManager(view.context)
    }

    private fun getDataFromFireStore(view: View) {
        var chatAdepter: ChatAdepter
        mFriendList = ArrayList<String>()
        Log.d("CHAT", "getData called")
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(mCurrentUser.uid).addSnapshotListener { value, error ->

            if (error != null) {
//                Toast.makeText(view.context,"Can't find any chat", Toast.LENGTH_SHORT).show()
            } else {
                mFriendList.clear()
                try {
                    val data = value?.data?.getValue("FriendList") as ArrayList<String>
                    Log.d("CHAT", "The Data are : $data")
                    for (i in data) {
                        mFriendList.add(i)
                    }
                    chatAdepter = ChatAdepter(mFriendList, mCurrentUser.uid, this)
                    recyclerView.adapter = chatAdepter
                } catch (e: Exception) {
                    Toast.makeText(view.context, "No Friends yet", Toast.LENGTH_SHORT).show()
                }

            }

        }

    }
    override fun onFriendViewClicked(myUID: String, friendUID: String, imageURL: String, userName: String) {
        val intent = Intent(requireView().context, ChatActivity::class.java)
        intent.putExtra("ownUID", myUID)
        intent.putExtra("friendUID", friendUID)
        intent.putExtra("ImageUrl", imageURL)
        intent.putExtra("UserName", userName)
        startActivity(intent)
    }

}