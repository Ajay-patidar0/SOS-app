package com.example.sosapp

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var firestore: FirebaseFirestore

    companion object {
        private const val SMS_PERMISSION_REQUEST_CODE = 101
        private const val LOCATION_PERMISSION_REQUEST_CODE = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set window insets for UI
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Firestore and Location Services
        firestore = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // SOS button click listener
        binding.sosbtn.setOnClickListener {
            if (checkPermissions()) {
                fetchLocationAndSendSOS("Emergency! Help needed!") // General SOS message
            }
        }

        // My Circle button click listener (Navigate to contacts)
        binding.mycircle.setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java))
        }

        // Medical button click listener
        binding.medical.setOnClickListener {
            if (checkPermissions()) {
                fetchLocationAndSendSOS("Medical emergency! Immediate assistance needed!", true)
            }
        }

        // Accident button click listener
        binding.accident.setOnClickListener {
            if (checkPermissions()) {
                fetchLocationAndSendSOS("Accident! Need help urgently!", true)
            }
        }

        // Violence button click listener
        binding.violence.setOnClickListener {
            if (checkPermissions()) {
                fetchLocationAndSendSOS("Violence incident! Please send help!")
            }
        }

        // Fire button click listener
        binding.fire.setOnClickListener {
            if (checkPermissions()) {
                fetchLocationAndSendSOS("Fire emergency! Immediate assistance required!")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocationAndSendSOS(customMessage: String, includeHospitals: Boolean = false) {
        fusedLocationClient.lastLocation.addOnSuccessListener(this, OnSuccessListener<Location> { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude

                Log.d("Location Fetch", "Latitude: $latitude, Longitude: $longitude")

                if (includeHospitals) {
                    fetchNearbyHospitals(latitude, longitude, customMessage)
                } else {
                    val messageWithLocation = "$customMessage Help needed at: https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
                    fetchContactsAndSendSOS(latitude, longitude, messageWithLocation)
                }
            } else {
                Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show()
            }
        }).addOnFailureListener { e ->
            Toast.makeText(this, "Location fetch error: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("Location Error", "Error fetching location", e)
        }
    }

    private fun fetchNearbyHospitals(latitude: Double, longitude: Double, customMessage: String) {
        val overpassService = Retrofit.Builder()
            .baseUrl("https://overpass-api.de/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassService::class.java)

        val query = "[out:json];node[amenity=hospital](around:2000,$latitude,$longitude);out;"
        overpassService.getNearbyHospitals(query).enqueue(object : Callback<HospitalResponse> {
            override fun onResponse(call: Call<HospitalResponse>, response: Response<HospitalResponse>) {
                if (response.isSuccessful) {
                    val hospitals = response.body()?.elements ?: emptyList()

                    // Get the top 5 nearest hospitals
                    val nearestHospitals = hospitals
                        .sortedBy { hospital ->
                            val hospitalLatitude = hospital.lat
                            val hospitalLongitude = hospital.lon
                            calculateDistance(latitude, longitude, hospitalLatitude, hospitalLongitude)
                        }
                        .take(5) // Take only the top 5 nearest hospitals

                    sendSOSMessageWithHospitals(latitude, longitude, nearestHospitals, customMessage)
                } else {
                    Log.e("Overpass API", "Error: ${response.errorBody()?.string()}")
                    fetchContactsAndSendSOS(latitude, longitude, customMessage) // Send without hospital info
                }
            }

            override fun onFailure(call: Call<HospitalResponse>, t: Throwable) {
                Log.e("Overpass API", "Failure: ${t.message}")
                fetchContactsAndSendSOS(latitude, longitude, customMessage) // Send without hospital info
            }
        })
    }

    private fun fetchContactsAndSendSOS(latitude: Double, longitude: Double, customMessage: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            firestore.collection("contacts").document(userId).collection("user_contacts")
                .get()
                .addOnSuccessListener { result ->
                    val contactList = result.toObjects(Contact::class.java)
                    Log.d("Contacts", "Fetched contacts: $contactList")

                    if (contactList.isNotEmpty()) {
                        sendCustomSOSMessage(latitude, longitude, contactList, customMessage)
                    } else {
                        Toast.makeText(this, "No contacts found!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error fetching contacts: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSOSMessageWithHospitals(latitude: Double, longitude: Double, hospitals: List<Hospital>, smsMessage: String) {
        // Create the emergency message with the user's location and nearby hospitals
        val hospitalLocations = hospitals.joinToString("\n") {
            "Hospital: ${it.tags.name ?: "Unknown"} - Location: https://www.google.com/maps/search/?api=1&query=${it.lat},${it.lon}"
        }

        // Log the hospital details for debugging
        Log.d("Hospital Locations", "Nearby Hospitals:\n$hospitalLocations")

        // Construct the complete message
        val message = "$smsMessage Help needed at: https://www.google.com/maps/search/?api=1&query=$latitude,$longitude\nNearby Hospitals:\n$hospitalLocations"

        // Log the final message before sending
        Log.d("SOS Message", "Final SOS Message: $message")

        // Fetch contacts to send the message
        fetchContactsAndSendSOS(latitude, longitude, message)
    }

    private fun sendCustomSOSMessage(latitude: Double, longitude: Double, contactList: List<Contact>, smsMessage: String) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.SEND_SMS), SMS_PERMISSION_REQUEST_CODE)
            return
        }

        val smsManager = SmsManager.getDefault()

        // Split message if it exceeds the limit
        val messageParts = smsManager.divideMessage(smsMessage)

        contactList.forEach { contact ->
            try {
                // Log the message being sent
                Log.d("SMS", "Sending message to: ${contact.phone} - Message: $smsMessage")

                // Send each part of the message
                smsManager.sendMultipartTextMessage(contact.phone, null, messageParts, null, null)

                Log.d("SMS", "Sent message to: ${contact.phone}")
            } catch (e: Exception) {
                Log.e("SMS Error", "Failed to send message to: ${contact.phone}, Error: ${e.message}")
            }
        }

        Toast.makeText(this, "SOS Message sent to all contacts!", Toast.LENGTH_SHORT).show()
    }



    private fun checkPermissions(): Boolean {
        val smsPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
        val locationPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)

        return if (smsPermission == PackageManager.PERMISSION_GRANTED && locationPermission == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.SEND_SMS, android.Manifest.permission.ACCESS_FINE_LOCATION),
                SMS_PERMISSION_REQUEST_CODE)
            false
        }
    }

    // Handle the result of permission requests
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            SMS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, can send SMS
                } else {
                    Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, can access location
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    interface OverpassService {
        @GET("interpreter")
        fun getNearbyHospitals(@Query("data") query: String): Call<HospitalResponse>
    }
}
