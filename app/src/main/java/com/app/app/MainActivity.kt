package com.app.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


class MainActivity : AppCompatActivity(), OnMapReadyCallback{

    //지도
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: MainActivity.MyLocationCallback
    //지도


    override fun onStart() {
        super.onStart()
        fetchLocation()
    }

    @SuppressLint("UnspecifiedImmutableFlag", "UseSwitchCompatOrMaterialCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intent3 = Intent(this, SubActivity::class.java)
        val imgbtn: ImageButton = findViewById(R.id.imgbtn)

        imgbtn.setOnClickListener{
            startActivity(intent3)
        }

        val intent2 = Intent(this, LoadActivity::class.java)
        startActivity(intent2)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        initLocation()


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    @SuppressLint("SetTextI18n")
    private fun fetchLocation() {
        val geocoder = Geocoder(this)
        val adr: TextView = findViewById(R.id.Adr)
        val task = fusedLocationProviderClient.lastLocation
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat
                .checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ){
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 101)
        }
        task.addOnSuccessListener {
            if (it != null){
                val address = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                adr.text = address[0].getAddressLine(0)
            }

        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }
    @SuppressLint("VisibleForTests")
    private fun initLocation(){
        fusedLocationProviderClient = FusedLocationProviderClient(this)

        locationCallback = MyLocationCallback()
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000  //5000밀리초 마다 위치 갱신
        locationRequest.fastestInterval = 5000
    }
    private fun LocationRequest(): LocationRequest {
        locationRequest = LocationRequest.create().apply {
            interval = 100
            fastestInterval = 50
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            maxWaitTime = 100
        }
        return locationRequest
    }

    override fun onResume() {
        super.onResume()
        addLocationListener()
    }

    private fun addLocationListener() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) //위치권한이 없을 경우, 권한을 부야하는 창을 띄웁니다.
            != PackageManager.PERMISSION_GRANTED && ActivityCompat
                .checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ){
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 101)
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    inner class MyLocationCallback:LocationCallback() {
        override fun onLocationResult(locationRequest: LocationResult?) {

            val location = locationRequest?.lastLocation

            location?.run {
                mMap.clear()
                val latLng = LatLng(latitude, longitude)
                mMap.addMarker(
                    MarkerOptions()
                    .snippet("자신의 위치")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                    .position(latLng).title("내 위치"))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
            }
        }

    }

    private var backPressedTime : Long = 0
    override fun onBackPressed() {
        if(System.currentTimeMillis() - backPressedTime < 2000){
            finish()
            return
        }
        Toast.makeText(this, "한번 더 누르시면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show()
        backPressedTime = System.currentTimeMillis()
    }
}