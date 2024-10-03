package com.example.sosapp

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
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
    companion object {
        private const val SMS_PERMISSION_REQUEST_CODE = 101
        private const val CALL_PERMISSION_REQUEST_CODE = 102
    }


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


     /*    binding.home.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } */

        binding.mycircle.setOnClickListener {
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
        // Step 1: Create the emergency message with location
        val message = "Emergency! Help needed at: https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
        val contactList: MutableList<Contact> = mutableListOf()
        // Step 2: Retrieve contacts from your contact list (Replace with your actual contact list)
        // Example contact list. You'll have to implement your own logic to retrieve actual contacts.
        val contacts = contactList.map { it.phone }

        // Step 3: Send SMS to all contacts
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.SEND_SMS), SMS_PERMISSION_REQUEST_CODE)
            return
        }

        // Step 4: Use SmsManager to send the message to each contact
        val smsManager = SmsManager.getDefault()
        for (contact in contacts) {
            smsManager.sendTextMessage(contact, null, message, null, null)
        }

        Toast.makeText(this, "SOS Message sent to all contacts!", Toast.LENGTH_SHORT).show()

        // Step 5: Call the priority contact if available
        val priorityContact = contactList.find { it.isPriority }
        if (priorityContact != null) {
            makeCall(priorityContact.phone)
        } else {
            Toast.makeText(this, "No priority contact set.", Toast.LENGTH_SHORT).show()
        }
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
        grantResults: IntArray,
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