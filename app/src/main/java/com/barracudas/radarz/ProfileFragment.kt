package com.barracudas.radarz

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.opengl.Visibility
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import com.barracudas.radarz.Data.OpenWeatherMapData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import id.zelory.compressor.Compressor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.lang.Exception

class ProfileFragment : Fragment() {

    lateinit var clint: FusedLocationProviderClient
    private lateinit var aboutEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var mCurrentUser: FirebaseUser
    private lateinit var ProgressBar: ProgressDialog
    private lateinit var mProfileImage: ImageView
    private lateinit var mCircularProgressBar: ProgressBar
    private lateinit var mNameInProfile: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clint = activity?.let { LocationServices.getFusedLocationProviderClient(it) }!!

        initialize(view)
        getDataFromFireStore()
        getCurrentLocation()
        nameEditText.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                showProgressBar()
                val db = FirebaseFirestore.getInstance()
                val name = nameEditText.text.toString()
                if (name != "") {
                    db.collection("users").document(mCurrentUser.uid)
                        .update(mapOf("Name" to name))
                        .addOnSuccessListener {
                            ProgressBar.dismiss()
                            Toast.makeText(view.context, "Updated Successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener{
                            ProgressBar.dismiss()
                            Toast.makeText(view.context, "Unable to Update", Toast.LENGTH_SHORT).show()
                        }
                }
                return@OnKeyListener true
            }
            false
        })

        aboutEditText.setOnKeyListener(
            View.OnKeyListener { v, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                    showProgressBar()
                    val db = FirebaseFirestore.getInstance()
                    val about = aboutEditText.text.toString()
                    if (about != "") {
                        db.collection("users").document(mCurrentUser.uid)
                            .update(mapOf("Status" to about))
                            .addOnSuccessListener {
                                ProgressBar.dismiss()
                                Toast.makeText(view.context, "Updated Successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener{
                                ProgressBar.dismiss()
                                Toast.makeText(view.context, "Unable to Update", Toast.LENGTH_SHORT).show()
                            }
                    }
                    return@OnKeyListener true
                }
                false
            })

        mProfileImage.setOnClickListener {
            startActivityForResult(
                Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                ), 1000
            )
        }

    }

    private fun initialize(view: View) {
        aboutEditText = view.findViewById(R.id.AboutProfile)
        nameEditText = view.findViewById(R.id.UserProfileName)
        mProfileImage = view.findViewById(R.id.ProfileImageInProfile)
        mCircularProgressBar = view.findViewById(R.id.progressBarInProfile)
        mNameInProfile = view.findViewById(R.id.NameInProfile)
        mCurrentUser = Firebase.auth.currentUser!!
    }

    private fun getDataFromFireStore() {
        var db = FirebaseFirestore.getInstance()
        db.collection("users").document(mCurrentUser.uid).get()
            .addOnSuccessListener {
                var data = it
                nameEditText.setText(it.data!!.getValue("Name").toString())
                aboutEditText.setText(it.data!!.getValue("Status").toString())
                val storageRef: StorageReference =
                    FirebaseStorage.getInstance("gs://radaz-3b522.appspot.com").getReference()
                storageRef.child("Images/${mCurrentUser.uid}.jpg").downloadUrl
                    .addOnSuccessListener {
                        Picasso.get().load(it).into(mProfileImage)
                        mCircularProgressBar.visibility = View.GONE
                    }
                    .addOnFailureListener {
                        Picasso.get().load(data.data!!.getValue("DP").toString())
                            .into(mProfileImage)
                        mCircularProgressBar.visibility = View.GONE
                    }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    if (data != null) {

                        val actualImage = FileUtil.from(requireView().context, data.data)
                        mProfileImage.setImageURI(data.data)

                        showProgressBar()

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
                    Toast.makeText(
                        requireView().context,
                        "Some Error Occurring",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        }
    }

    private fun updateImageInFirebase(image: ByteArray) {

        val url = "Images/${mCurrentUser.uid}.jpg"
        val storageRef: StorageReference = FirebaseStorage.getInstance().reference.child(url)
        if (image != null) {
            storageRef.putBytes(image)
                .addOnSuccessListener {
                    ProgressBar.dismiss()
                    Toast.makeText(
                        requireView().context,
                        "Updated Successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener {
                    ProgressBar.dismiss()
                    Toast.makeText(
                        requireView().context,
                        "Updated Failed. Please Try again",
                        Toast.LENGTH_SHORT
                    ).show()

                }
        }
    }

    private fun showProgressBar() {
        ProgressBar = ProgressDialog(requireView().context)
        ProgressBar.setTitle("Updating")
        ProgressBar.setMessage("Please wait while we updating your details")
        ProgressBar.setCanceledOnTouchOutside(false)
        ProgressBar.show()
    }

    private fun getCurrentLocation() {
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/")
            .build()
            .create(ApiInterface::class.java)

        if (context?.let {
                ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } != PackageManager.PERMISSION_GRANTED && context?.let {
                ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            } != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        var task: Task<Location> = clint.lastLocation
        task.addOnSuccessListener {

            var mLatitude = it.latitude.toString()
            var mLongitude = it.longitude.toString()

            val retrofitData = retrofitBuilder.getData(mLatitude, mLongitude)
            retrofitData.enqueue(object : Callback<OpenWeatherMapData?> {
                override fun onResponse(
                    call: Call<OpenWeatherMapData?>,
                    response: Response<OpenWeatherMapData?>
                ) {
                    Log.d("DATA", "The data is : ${response.body()!!.name}")
                    mNameInProfile.text = response.body()!!.name
                }

                override fun onFailure(call: Call<OpenWeatherMapData?>, t: Throwable) {

                }
            })

        }
    }
}