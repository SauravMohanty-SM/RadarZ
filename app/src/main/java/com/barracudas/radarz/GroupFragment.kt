package com.barracudas.radarz

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import id.zelory.compressor.Compressor
import java.io.ByteArrayOutputStream
import java.lang.Exception

// TODO: Rename parameter arguments, choose names that match

class GroupFragment : Fragment() {


    // TODO: Declaration of variables
    private lateinit var AddGroupButtom : ImageView
    private lateinit var database : FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var dataList: ArrayList<String>
    private lateinit var lastMessage: ArrayList<String>
    private lateinit var lastTime: ArrayList<String>
    private var Check: Int = 0
    private lateinit var GroupName: String
    private lateinit var progressBar: ProgressBar
    private lateinit var ProgressBar : ProgressDialog
    private lateinit var mImageView: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //TODO: Function for initialize variables
        initialize(view)

        //TODO: Group Add Buttom function
        AddGroupButtom.setOnClickListener {
            createGroup(view)
        }

        //TODO: Function for display Groups in recycle view
        displayGroups(view)

    }

    //TODO: Function for initialize the variables
    private fun initialize(view: View) {
        AddGroupButtom = view.findViewById(R.id.AddBottomGroup)
        database = FirebaseDatabase.getInstance()
        reference = database.getReference("Groups")
        recyclerView = view.findViewById(R.id.recycleViewGroup)
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        progressBar = view.findViewById(R.id.progressBarInGroupFragment)
    }

    //TODO: Function to create group
    private fun createGroup(view: View) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle("Enter Group Name")
        val editText: EditText = EditText(context)
        editText.hint = "e.g/ Coder"

        builder.setView(editText)

        //TODO: Buttom for Create group on Database
        builder.setPositiveButton("Create", DialogInterface.OnClickListener { dialog, which ->
            var GroupName : String = editText.text.toString()

            for (i in dataList) {
                if (GroupName == i) {
                    Check += 1
                }
            }

            if (GroupName != "" && Check == 0) {

                val map:HashMap<String, String> = HashMap()
                map.put("LastMessage", " ")
                map.put("LastTime", " ")
                reference.child(GroupName).child("LastDetails").setValue(map).addOnCompleteListener {

                    if (it.isSuccessful) {
                        Toast.makeText(view.context, "Group created successfully", Toast.LENGTH_SHORT)
                            .show()

                    } else {
                        Toast.makeText(context, "Failed. Please Try again", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } else {
                Toast.makeText(view.context, "Group name can't be null or duplicate", Toast.LENGTH_SHORT).show()
            }


        })

        builder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->

        })

        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    //TODO: Function to display groups in recycle view
    private fun displayGroups(view: View) {

        var adepter: GroupsAdapter

        //TODO: Listen when data changed
        val postListenr = object : ValueEventListener, GroupClicked, ImageClicked {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("LOG", "The data is : $snapshot")

                AddGroupButtom.visibility = View.VISIBLE
                dataList = ArrayList<String>()
                lastMessage = ArrayList<String>()
                lastTime = ArrayList<String>()

                if(snapshot.exists()) {

                    for (groups in snapshot.children) {
                        val message = groups.child("LastDetails").child("LastMessage").value.toString()
                        val time = groups.child("LastDetails").child("LastTime").value.toString()
                        val group = groups.key
                        lastMessage.add(message)
                        lastTime.add(time)
                        dataList.add(group!!)
                        Log.d("LOG", "The groups are $group")
                    }
                    progressBar.visibility = View.GONE
                    adepter = GroupsAdapter(dataList, lastMessage, lastTime,this, this)
                    recyclerView.adapter = adepter
                }
            }


            override fun onCancelled(error: DatabaseError) {
                AddGroupButtom.visibility = View.VISIBLE
                Toast.makeText(view.context, "Unable to fetch data", Toast.LENGTH_SHORT).show()
            }

            // TODO: Function call on Recycle view clicked
            override fun onItemClicked(item: String) {
                Log.d("LOG", "The Group name is $item")
                val intent = Intent(view.context, MessaginActivity::class.java)
                intent.putExtra("GroupName", item)
                startActivity(intent)
            }

            override fun OnImageClicked(item: String, image: ImageView) {

                GroupName = item
                mImageView = image

                startActivityForResult(
                    Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    ), 1000
                )
            }


        }

        reference.addValueEventListener(postListenr)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000) {
            if (resultCode == RESULT_OK) {
                try {
                    if (data != null) {

                        val actualImage = FileUtil.from(requireView().context, data.data)
                        mImageView.setImageURI(data.data)

                        ProgressBar = ProgressDialog(requireView().context)
                        ProgressBar.setTitle("Updating Image")
                        ProgressBar.setMessage("Please wait while we updating your Image")
                        ProgressBar.setCanceledOnTouchOutside(false)
                        ProgressBar.show()

                        val compressedImage = Compressor(requireView().context)
                            .setMaxWidth(640)
                            .setMaxHeight(480)
                            .setQuality(75)
                            .compressToBitmap(actualImage)

                        val baos = ByteArrayOutputStream()
                        compressedImage.compress(Bitmap.CompressFormat.JPEG, 50, baos)
                        val image = baos.toByteArray()

                        updateImageInFirebase(image)
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireView().context, "Some Error Occurring", Toast.LENGTH_SHORT).show()
                }

            }
        }
    }

    private fun updateImageInFirebase(image: ByteArray) {

        val url = "Images/$GroupName.jpg"
        val storageRef: StorageReference = FirebaseStorage.getInstance().reference.child(url)
        if (image != null) {
            storageRef.putBytes(image)
                .addOnSuccessListener {
                    ProgressBar.dismiss()
                Toast.makeText(requireView().context, "Updated Successfully", Toast.LENGTH_SHORT).show()
            }
                .addOnFailureListener {
                    ProgressBar.dismiss()
                    Toast.makeText(requireView().context, "Updated Failed. Please Try again", Toast.LENGTH_SHORT).show()

                }
        }
    }
}