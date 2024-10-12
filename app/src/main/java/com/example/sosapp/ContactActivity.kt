package com.example.sosapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sosapp.databinding.ActivityContactBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class ContactActivity : AppCompatActivity() {
    private val contactList = ArrayList<Contact>()
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var binding: ActivityContactBinding
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firestore with offline persistence
        firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        // Set up RecyclerView
        binding.recyclerViewContacts.layoutManager = LinearLayoutManager(this)
        contactAdapter = ContactAdapter(contactList)
        binding.recyclerViewContacts.adapter = contactAdapter

        // Load contacts from Firestore
        loadContacts()

        // Set onClick listeners
        binding.addcontact.setOnClickListener {
            showAddContactDialog()
        }

        binding.homebtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        // Ensure the user is authenticated before loading contacts
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            loadContacts() // Load contacts if user is authenticated
        } else {
            Log.w("ContactActivity", "User not authenticated.")
            // Optionally, redirect to a login activity
        }
    }

    private fun showAddContactDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.editTextName)
        val phoneEditText = dialogView.findViewById<EditText>(R.id.editTextPhone)

        AlertDialog.Builder(this)
            .setTitle("Add Contact")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameEditText.text.toString().trim()
                val phone = phoneEditText.text.toString().trim()


                // Validation for phone number
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    if (phone.length == 10 && phone.all { it.isDigit() }) {
                        // If a contact is marked as a priority, remove priority from existing contacts

                        val newContact = Contact(name, phone)
                        contactList.add(newContact)
                        contactAdapter.notifyDataSetChanged() // Notify adapter about new contact

                        // Save new contact to Firestore
                        saveContactToFirestore(newContact)
                    } else {
                        Toast.makeText(this, "Phone number must be 10 digits.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }


    private fun saveContactToFirestore(contact: Contact) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val contactData = hashMapOf(
                "name" to contact.name,
                "phone" to contact.phone,
            )

            firestore.collection("contacts").document(userId).collection("user_contacts")
                .add(contactData)
                .addOnSuccessListener { documentReference ->
                    Log.d("ContactActivity", "Contact added with ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.w("ContactActivity", "Error adding contact", e)
                }
        } else {
            Log.w("ContactActivity", "User not authenticated.")
        }
    }



    private fun loadContacts() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            firestore.collection("contacts")
                .document(userId) // Access the document for the current user
                .collection("user_contacts") // Access the user's contacts sub-collection
                .get()
                .addOnSuccessListener { result ->
                    contactList.clear() // Clear existing contacts
                    var hasPriorityContact = false // Track if there is a priority contact

                    // Iterate through the retrieved documents
                    for (document in result) {
                        val contact = document.toObject(Contact::class.java)
                        contactList.add(contact)


                    }

                    // Notify adapter about data changes
                    contactAdapter.notifyDataSetChanged()



                }
                .addOnFailureListener { exception ->
                    // Handle errors while fetching data
                    Log.e("LoadContacts", "Error getting contacts: ", exception)
                    Toast.makeText(this, "Failed to load contacts.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }


}
