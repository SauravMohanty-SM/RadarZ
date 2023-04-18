package com.barracudas.radarz


import android.Manifest
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import java.lang.Exception


class SearchOnMapFragment : Fragment(), AddBottomClicked {

    lateinit var smf: SupportMapFragment
    lateinit var clint: FusedLocationProviderClient
    private lateinit var mImageUrl: ArrayList<String>
    private lateinit var mStatus: ArrayList<String>
    private lateinit var mName: ArrayList<String>
    private lateinit var mUID: ArrayList<String>
    private lateinit var recyclerView: RecyclerView
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mCurrentUser: FirebaseUser
    private lateinit var mProgressBar: ProgressBar


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search_on_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        smf = childFragmentManager.findFragmentById(R.id.googleMapSearch) as SupportMapFragment
        clint = activity?.let { LocationServices.getFusedLocationProviderClient(it) }!!

        recyclerView = view.findViewById(R.id.recyclerViewForMap)
        mProgressBar = view.findViewById(R.id.progressBarInMap)
        recyclerView.layoutManager = LinearLayoutManager(view.context)

        mAuth = Firebase.auth
        mCurrentUser = mAuth.currentUser!!

        getLocationFromMaps()
    }

    private fun getLocationFromMaps() {

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

            try {
                var mLatitude = it.latitude.toString()
                var mLongitude = it.longitude.toString()

                var latlng: LatLng = LatLng(it.latitude,it.longitude)

                smf.getMapAsync(OnMapReadyCallback {
                    var markerOption:MarkerOptions=MarkerOptions().position(latlng).title("You are here")

                    it.addMarker(markerOption)
                    it.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng,11.8F))
                    getDataFromFireStore(mLatitude,mLongitude)
                })
            } catch (e: Exception) {
                Toast.makeText(context, "Please turn on Your Location", Toast.LENGTH_SHORT).show()
            }


        }

    }

    private fun getDataFromFireStore(mLatitude: String, mLongitude: String) {
        var adepter: MapAdepter

        mImageUrl = ArrayList<String>()
        mStatus = ArrayList<String>()
        mName = ArrayList<String>()
        mUID = ArrayList<String>()

        var OwnLatitude = mLatitude.toDouble()
        var OwnLongitude = mLongitude.toDouble()
        var FriendLatitude: Double
        var FriendLongitude: Double
        var FriendUID: String

        val db = FirebaseFirestore.getInstance()
        db.collection("users").get()
            .addOnSuccessListener {
                for (document in it.documents) {
                    Log.d("MAP", "The Map are : $it")

                    try {
                        FriendLatitude = document.data?.getValue("Latitude") as Double
                        FriendLongitude = document.data?.getValue("Longitude") as Double
                    } catch (e: Exception) {
                        FriendLatitude = 0.0
                        FriendLongitude = 0.0
                    }

                    FriendUID = document.data?.getValue("Uid").toString()

                    if (FriendLatitude-0.106 <= OwnLatitude && OwnLatitude <= FriendLatitude+0.106 &&
                        FriendLongitude-0.106 <= OwnLongitude && OwnLongitude <= FriendLongitude+0.106 &&
                            mCurrentUser.uid != FriendUID) {
                                mImageUrl.add(document.data?.getValue("DP").toString())
                                mStatus.add(document.data?.getValue("Status").toString())
                                mName.add(document.data?.getValue("Name").toString())
                                mUID.add(document.data?.getValue("Uid").toString())
                                showPeopleOnMap(FriendLatitude, FriendLongitude, document.data?.getValue("Name").toString())
                    }
                }
                mProgressBar.visibility = View.GONE
                adepter = MapAdepter(mImageUrl, mStatus, mName, mUID, mCurrentUser.uid ,this)
                recyclerView.adapter = adepter
            }
    }

    private fun showPeopleOnMap(friendLatitude: Double, friendLongitude: Double, name: String) {
        try {
            var latlng: LatLng = LatLng(friendLatitude,friendLongitude)

            smf.getMapAsync(OnMapReadyCallback {
                var markerOption: MarkerOptions = MarkerOptions().position(latlng).title(name)

                it.addMarker(markerOption)
                it.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 11.8F))
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Please turn on Your Location", Toast.LENGTH_SHORT).show()
        }
    }

    override fun whenAddBottomClicked(friendUID: String, Image: ImageView) {

        val progressBar = ProgressDialog(requireView().context)
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
            db.collection("users").document(mCurrentUser.uid).update("FriendList",FieldValue.arrayUnion(friendUID))
                .addOnSuccessListener {
                    db.collection("users").document(friendUID).
                    update("FriendList", FieldValue.arrayUnion(mCurrentUser.uid))
                        .addOnSuccessListener {
                            progressBar.dismiss()
                            Toast.makeText(requireView().context, "Added to your friend", Toast.LENGTH_SHORT).show()
                            Image.visibility = View.GONE
                        }
                        .addOnFailureListener {
                            progressBar.dismiss()
                            Toast.makeText(requireView().context, "Failed to add friend", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
    }
}