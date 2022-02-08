package com.barracudas.radarz

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.UserDictionary.Words
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.common.base.MoreObjects
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlin.properties.Delegates
import android.provider.UserDictionary.Words.APP_ID
import com.google.android.gms.location.*
import com.squareup.picasso.Picasso
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), PermissionListener {

    // TODO : Declare all the variables
    private lateinit var imageView :ImageView
    private lateinit var progress: ProgressBar
    private lateinit var mAuth : FirebaseAuth
    private var mCurrentUser : FirebaseUser?= null
    private lateinit var FirebaseDatabase : FirebaseFirestore
    private lateinit var personName: String
    private lateinit var personImage: Uri
    private lateinit var mLatitude: String
    private lateinit var mLongitude: String
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initialize()

        // TODO : Check the Location Permission
        Dexter.withContext(applicationContext)
            .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(this)
            .check()

        // TODO : Finding the user name from the gmail
        val acct = GoogleSignIn.getLastSignedInAccount(applicationContext)
        if (acct != null) {
            personName = acct.displayName
            personImage = acct.photoUrl
            Log.d("LOG", "Image = " + personImage)
        }

    }

    override fun onResume() {
        super.onResume()
        locationGether()
    }

    // TODO : Function requried for Dexter to check Location  Permission
    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
        startVideo()
    }


    // TODO : Function requried for Dexter to check Location  Permission
    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
        Toast.makeText(this, "Please allow Location Permission", Toast.LENGTH_SHORT).show()
        finish()
    }

    // TODO : Function requried for Dexter to check Location  Permission
    override fun onPermissionRationaleShouldBeShown(p0: PermissionRequest?, p1: PermissionToken?) {
        p1?.continuePermissionRequest()
    }

    // TODO : The Animation is starting and delay the time for animation
    private fun startVideo() {

        imageView.alpha = 0.0001f
        imageView.animate().setDuration(3000).alpha(1f).withEndAction {
            progress.visibility = View.VISIBLE
            delayTime()
        }
    }

    // TODO : Initialize all the variables
    private fun initialize() {
        imageView = findViewById(R.id.logo)
        progress = findViewById(R.id.progressBarMainActivity)
        mAuth = Firebase.auth
        mCurrentUser = mAuth.currentUser
        FirebaseDatabase = FirebaseFirestore.getInstance()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun delayTime() {

        // TODO : Checking the LogIn Authenticate user
        if (mCurrentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {

            //TODO: Function For getLocation
            try {
                if (mLatitude != "s" && mLongitude != "s") {
                    retriveDataFromFIreStore()
                } else {
                    Toast.makeText(this, "Can't fetch location. Check every thing and try again", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Can't fetch location. Check every thing and try again", Toast.LENGTH_LONG).show()
                finish()
            }


        }
    }

    private fun locationGether() {
        locationRequest = LocationRequest().apply {

            interval = TimeUnit.SECONDS.toMillis(60)

            fastestInterval = TimeUnit.SECONDS.toMillis(30)

            maxWaitTime = TimeUnit.MINUTES.toMillis(2)

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }


        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                locationResult?.lastLocation?.let {
                    mLatitude = it.latitude.toString()
                    mLongitude = it.longitude.toString()

                    if (mLatitude == "s" && mLongitude == "s") {
                        mLatitude = "s"
                        mLongitude = "s"
                        locationGether()
                    }

                } ?: {
                    Log.d("TAG", "Location information isn't available.")
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    // TODO : Receiving the data from the Firestore data base
    private fun retriveDataFromFIreStore() {
        FirebaseDatabase.collection("users")
            .document(mCurrentUser?.uid!!)
            .get()
            .addOnSuccessListener { result ->

                try {
                    val name = result.data?.getValue("Name")
                    Log.d("LOG", "The Name is " + name)
                    Log.d("LOG", "In method Latitute: $mLatitude ; Longitute: $mLongitude")

                    if (name == null) {

                        // TODO : Function to add data to fire store with all details
                            addDetailsToFirebase(mLatitude, mLongitude)
                    } else {

                        // TODO : Function to update data stored in the fire store
                            addDetailsToFirebaseWithoutName(mLatitude, mLongitude)
                    }
                } catch (e : Exception) {

                }


            }
            .addOnFailureListener { exception ->
                Log.w("LOG", "Error getting documents.", exception)
            }
    }


    // TODO : Function to update data stored in the fire store
    private fun addDetailsToFirebaseWithoutName(mLatitude: String, mLongitude: String) {

        Log.d("LOG", "Entered to addDetails to firebase without name")

        FirebaseDatabase.collection("users")
            .document(mCurrentUser?.uid!!)
            .update(mapOf("Latitude" to mLatitude.toDouble(),
                            "Longitude" to mLongitude.toDouble()))
            .addOnSuccessListener {
                Log.d("LOG", "Data add to database successful")

                //TODO: Finally the user get access
                removeLocation()
                startActivity(Intent(this, MainActivityForUser::class.java))
                finish()
            } .addOnFailureListener {
                Log.d("LOG", "Data add to database failed", it)
            }
    }


    // TODO : Function to add data to fire store with all details
    private fun addDetailsToFirebase(mLatitude: String, mLongitude: String) {

        Log.d("LOG", "Entered to addDetails to firebase")


        val userDetails = hashMapOf(
            "Name" to personName,
            "Latitude" to mLatitude.toDouble(),
            "Longitude" to mLongitude.toDouble(),
            "Status" to "Available",
            "Uid" to mCurrentUser?.uid,
            "DP" to personImage.toString()
        )


        FirebaseDatabase.collection("users")
            .document(mCurrentUser?.uid!!)
            .set(userDetails)
            .addOnSuccessListener {
                Log.d("LOG", "Data add to database successful")

                //TODO: Finally the user get access
                removeLocation()
                startActivity(Intent(this, MainActivityForUser::class.java))
                finish()
            } .addOnFailureListener {
                Log.d("LOG", "Data add to database failed", it)
            }
    }

    private fun removeLocation() {
        val removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        removeTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("TAG", "Location Callback removed.")
            } else {
                Log.d("TAG", "Failed to remove Location Callback.")
            }
        }
    }

}