package com.example.sosapp

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.*
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firestore = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.sosbtn.setOnClickListener {
            if (checkPermissions()) {
                fetchLocationAndSendSOS("Emergency! Help needed!")
            }
        }

        binding.mycircle.setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java))
        }

        binding.medical.setOnClickListener {
            if (checkPermissions()) {
                fetchLocationAndSendSOS("Medical emergency! Immediate assistance needed!", true)
            }
        }

        binding.accident.setOnClickListener {
            if (checkPermissions()) {
                fetchLocationAndSendSOS("Accident! Need help urgently!", true)
            }
        }

        binding.violence.setOnClickListener {
            if (checkPermissions()) {
                fetchLocationAndSendSOS("Violence incident! Please send help!")
            }
        }

        binding.fire.setOnClickListener {
            if (checkPermissions()) {
                fetchLocationAndSendSOS("Fire emergency! Immediate assistance required!")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocationAndSendSOS(customMessage: String, includeHospitals: Boolean = false) {
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                val messageWithLocation = "$customMessage Help needed at: https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"

                if (includeHospitals && isInternetAvailable()) {
                    fetchNearbyHospitals(latitude, longitude, customMessage)
                } else {
                    fetchContactsAndSendSOS(latitude, longitude, messageWithLocation)
                }
            } else {
                val locationManager = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
                if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                    locationManager.requestSingleUpdate(
                        android.location.LocationManager.GPS_PROVIDER,
                        object : android.location.LocationListener {
                            override fun onLocationChanged(gpsLocation: Location) {
                                val latitude = gpsLocation.latitude
                                val longitude = gpsLocation.longitude
                                val messageWithLocation = "$customMessage Help needed at: https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"

                                if (includeHospitals && isInternetAvailable()) {
                                    fetchNearbyHospitals(latitude, longitude, customMessage)
                                } else {
                                    fetchContactsAndSendSOS(latitude, longitude, messageWithLocation)
                                }
                            }

                            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                            override fun onProviderEnabled(provider: String) {}
                            override fun onProviderDisabled(provider: String) {}
                        },
                        null
                    )
                } else {
                    Toast.makeText(this, "GPS is disabled. Please enable it.", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Location fetch error: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    val nearestHospitals = hospitals.sortedBy {
                        calculateDistance(latitude, longitude, it.lat, it.lon)
                    }.take(5)

                    sendSOSMessageWithHospitals(latitude, longitude, nearestHospitals, customMessage)
                } else {
                    fetchContactsAndSendSOS(
                        latitude,
                        longitude,
                        "$customMessage Help needed at: https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
                    )
                }
            }

            override fun onFailure(call: Call<HospitalResponse>, t: Throwable) {
                fetchContactsAndSendSOS(
                    latitude,
                    longitude,
                    "$customMessage Help needed at: https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
                )
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
        val hospitalLocations = hospitals.joinToString("\n") {
            "Hospital: ${it.tags.name ?: "Unknown"} - https://www.google.com/maps/search/?api=1&query=${it.lat},${it.lon}"
        }

        val message = "$smsMessage Help needed at: https://www.google.com/maps/search/?api=1&query=$latitude,$longitude\nNearby Hospitals:\n$hospitalLocations"
        fetchContactsAndSendSOS(latitude, longitude, message)
    }

    private fun sendCustomSOSMessage(latitude: Double, longitude: Double, contactList: List<Contact>, smsMessage: String) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.SEND_SMS), SMS_PERMISSION_REQUEST_CODE)
            return
        }

        val smsManager = SmsManager.getDefault()
        val messageParts = smsManager.divideMessage(smsMessage)

        contactList.forEach { contact ->
            try {
                smsManager.sendMultipartTextMessage(contact.phone, null, messageParts, null, null)
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
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.SEND_SMS, android.Manifest.permission.ACCESS_FINE_LOCATION),
                SMS_PERMISSION_REQUEST_CODE
            )
            false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            SMS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
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

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
    }

    interface OverpassService {
        @GET("interpreter")
        fun getNearbyHospitals(@Query("data") query: String): Call<HospitalResponse>
    }
}
