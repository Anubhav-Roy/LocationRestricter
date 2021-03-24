package roy.anubhav.awignassignment

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import roy.anubhav.awignassignment.databinding.ActivityMapsBinding
import java.lang.Exception

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnSuccessListener<LocationSettingsResponse>, OnFailureListener, GoogleMap.OnMarkerDragListener {


    lateinit var mGoogleMap: GoogleMap
    var mapFrag: SupportMapFragment? = null
    lateinit var mLocationRequest: LocationRequest
    var mLastLocation: Location? = null

    private var mCurrLocationMarker: Marker? = null
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    private val circleRadius = 5000.0 //In meters

    lateinit var binding: ActivityMapsBinding

    private var mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val locationList = locationResult.locations
            if (locationList.isNotEmpty()) {
                //The last location in the list is the newest
                val location = locationList.last()

                if(mLastLocation!=null){
                    val results = FloatArray(1)
                    Location.distanceBetween(mLastLocation!!.latitude, mLastLocation!!.longitude,
                            location.latitude, location.longitude, results);

                    //Roughly same position
                    if(results[0]<30)
                        return;
                }

                mLastLocation = location
                //Place current location marker
                val latLng = LatLng(location.latitude, location.longitude)
                addMarketToMap(latLng)
                lastMarkerPosition = latLng

                mGoogleMap.addCircle(
                        CircleOptions()
                                .center(latLng)
                                .radius(circleRadius)
                                .strokeColor(Color.argb(128, 0, 0, 255))
                                .fillColor(Color.argb(128, 0, 0, 255))
                                .clickable(false)
                )
                //move map camera
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11.0F))
            }
        }
    }

    private fun addMarketToMap(latLng: LatLng) {
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker?.remove()
        }
        val markerOptions = MarkerOptions()
        markerOptions.position(latLng)
        markerOptions.draggable(true)
        markerOptions.title("Current Position")

        mCurrLocationMarker = mGoogleMap.addMarker(markerOptions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_maps)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        supportActionBar?.title = "Map Activity"

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mapFrag = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFrag?.getMapAsync(this)
    }

    public override fun onPause() {
        super.onPause()

        //stop location updates when Activity is no longer active
        mFusedLocationClient?.removeLocationUpdates(mLocationCallback)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        mGoogleMap.mapType = GoogleMap.MAP_TYPE_HYBRID

        mGoogleMap.setOnMarkerDragListener(this);
        mLocationRequest = LocationRequest()
        mLocationRequest.interval = 120000 // two minute interval
        mLocationRequest.fastestInterval = 120000
        mLocationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
            ) {
                //Location Permission already granted
                println("MapsActivity.onMapReady")
                mFusedLocationClient?.let { checkLocationService(it,this,this) }

            } else {
                //Request Location Permission
                checkLocationPermission()
            }
        } else {
            mFusedLocationClient?.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
            mGoogleMap.isMyLocationEnabled = true
        }
    }

    private fun createLocationRequest() :LocationRequest{
        val mLocationRequest = LocationRequest();
        mLocationRequest.setInterval(200000);
        mLocationRequest.setFastestInterval(300000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    fun checkLocationService( client:FusedLocationProviderClient,
                                    successListener: OnSuccessListener<LocationSettingsResponse>,
                                    failureListener: OnFailureListener) {

        println("MapsActivity.checkLocationService")
        val request = createLocationRequest();
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(request);

        val settingsClient = LocationServices.getSettingsClient(this);
        val task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this) {
                println("MapsActivity.onSuccess")
                startLocationService(client, request, LocationCallback());
                successListener.onSuccess(it);
        };

        task.addOnFailureListener(this, failureListener);

    }

    @SuppressLint("MissingPermission")
    fun startLocationService(client: FusedLocationProviderClient, request: LocationRequest, locationCallback: LocationCallback) {
        client?.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
        mGoogleMap.isMyLocationEnabled = true
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton(
                                "OK"
                        ) { _, _ ->
                            //Prompt the user once explanation has been shown
                            ActivityCompat.requestPermissions(
                                    this@MapsActivity,
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                    MY_PERMISSIONS_REQUEST_LOCATION
                            )
                        }
                        .create()
                        .show()


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        MY_PERMISSIONS_REQUEST_LOCATION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                    ) {

                        mFusedLocationClient?.requestLocationUpdates(
                                mLocationRequest,
                                mLocationCallback,
                                Looper.myLooper()
                        )
                        mGoogleMap.setMyLocationEnabled(true)
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
//                        Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show()
                }
                return
            }
        }// other 'case' lines to check for other
        // permissions this app might request
    }

    companion object {
        val MY_PERMISSIONS_REQUEST_LOCATION = 99
    }

    override fun onSuccess(p0: LocationSettingsResponse?) {
    }

    override fun onFailure(p0: Exception) {
        Toast.makeText(this,"GPS is turned off",Toast.LENGTH_LONG).show()
    }

    lateinit var lastMarkerPosition: LatLng
    override fun onMarkerDragStart(p0: Marker?) {
    }

    override fun onMarkerDrag(p0: Marker?) {


    }

    override fun onMarkerDragEnd(p0: Marker?) {

        if (p0 != null) {

            val results = FloatArray(1)
            Location.distanceBetween(mLastLocation!!.latitude, mLastLocation!!.longitude,
                    p0.position.latitude, p0.position.longitude, results);

            if(results[0]>circleRadius){
                addMarketToMap(lastMarkerPosition)
                Toast.makeText(this,"Out of circle",Toast.LENGTH_LONG).show()
            }else
                lastMarkerPosition = p0.position
        }
    }
}