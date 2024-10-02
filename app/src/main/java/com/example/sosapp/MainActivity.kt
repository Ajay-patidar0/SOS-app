package com.example.sosapp

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sosapp.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.sosbtn.setOnClickListener {
            if(checkPermissions()){
                fetchLocationandSendSOS()
            }
        }


        binding.home.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.contacts.setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java))
            finish()
        }

    }

    @SuppressLint("MissingPermission")
    private fun fetchLocationandSendSOS() {
        fusedLocationClient.lastLocation.addOnSuccessListener(this, OnSuccessListener<Location>{
            location ->
            if(location !=null){
                val latitude = location.latitude
                val longitude = location.longitude
                sendSOSMessage(latitude, longitude)
            }
        })
    }

    private fun sendSOSMessage(latitude: Double, longitude: Double) {
                val message="Emergency! Help needed at: https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"

    // Retrieve contacts from your contact list (this is a placeholder)
        val contacts = listOf("1234567890")  // Add logic to retrieve actual contacts
        TODO()

    }

    private fun makeCall(phonenumber: String){
        val callIntent =Intent(Intent.ACTION_CALL)
        callIntent.data= Uri.parse("tel:$phonenumber")
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.CALL_PHONE)==PackageManager.PERMISSION_GRANTED){
            startActivity(callIntent)
        }else{
            Toast.makeText(this, "Call permission is not granterd", Toast.LENGTH_SHORT).show( )
        }
    }

    private fun checkPermissions(): Boolean {
        val smsPermission =
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
        val locationPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )

        return if(smsPermission == PackageManager.PERMISSION_GRANTED &&
            locationPermission == PackageManager.PERMISSION_GRANTED
        ) {
            true
        }
        else{
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.SEND_SMS,android.Manifest.permission.ACCESS_FINE_LOCATION),
                101)
            false
    }

    }

    // Handle the result of permission requests
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationandSendSOS()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

}