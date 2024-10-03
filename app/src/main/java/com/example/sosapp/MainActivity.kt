package com.example.sosapp

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sosapp.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var firestore: FirebaseFirestore

    companion object {
        private const val SMS_PERMISSION_REQUEST_CODE = 101
        private const val CALL_PERMISSION_REQUEST_CODE = 102
        private const val LOCATION_PERMISSION_REQUEST_CODE = 103
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(binding.root)

        // Set window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Firestore and Location Services
        firestore = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // SOS button click
        binding.sosbtn.setOnClickListener {
            if (checkPermissions()) {
                fetchLocationAndSendSOS()
            }
        }

        // My Circle button click (Navigate to contacts)
        binding.mycircle.setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java))
            finish()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocationAndSendSOS() {
        fusedLocationClient.lastLocation.addOnSuccessListener(this, OnSuccessListener<Location> { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                // Fetch contacts and then send the SOS message
                fetchContactsAndSendSOS(latitude, longitude)
            } else {
                Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchContactsAndSendSOS(latitude: Double, longitude: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            firestore.collection("contacts").document(userId).collection("user_contacts")
                .get()
                .addOnSuccessListener { result ->
                    val contactList = mutableListOf<Contact>()
                    for (document in result) {
                        val contact = document.toObject(Contact::class.java)
                        contactList.add(contact)
                        // Log the contact details for debugging
                        Log.d("Contact", "Fetched contact: $contact")
                    }
                    sendSOSMessage(latitude, longitude, contactList)
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error fetching contacts: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSOSMessage(latitude: Double, longitude: Double, contactList: List<Contact>) {
        // Create the emergency message with the user's location
        val message = "Emergency! Help needed at: https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.SEND_SMS), SMS_PERMISSION_REQUEST_CODE)
            return
        }

        // Use SmsManager to send the message to each contact
        val smsManager = SmsManager.getDefault()
        for (contact in contactList) {
            try {
                smsManager.sendTextMessage(contact.phone, null, message, null, null)
                Log.d("SMS", "Sent message to: ${contact.phone}")
            } catch (e: Exception) {
                Log.e("SMS Error", "Failed to send message to: ${contact.phone}, Error: ${e.message}")
            }
        }

        Toast.makeText(this, "SOS Message sent to all contacts!", Toast.LENGTH_SHORT).show()

        // Call the priority contact if available
        val priorityContact = contactList.find { it.isPriority }
        if (priorityContact != null) {
            makeCall(priorityContact.phone)
        } else {
            Toast.makeText(this, "No priority contact set.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeCall(phoneNumber: String) {
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:$phoneNumber")

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(callIntent)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CALL_PHONE), CALL_PERMISSION_REQUEST_CODE)
        }
    }

    private fun checkPermissions(): Boolean {
        val smsPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
        val locationPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)

        return if (smsPermission == PackageManager.PERMISSION_GRANTED && locationPermission == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.SEND_SMS, android.Manifest.permission.ACCESS_FINE_LOCATION),
                SMS_PERMISSION_REQUEST_CODE
            )
            false
        }
    }

    // Handle the result of permission requests
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            SMS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fetchLocationAndSendSOS()
                } else {
                    Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            CALL_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, you may call the priority contact here if needed
                } else {
                    Toast.makeText(this, "Call permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
