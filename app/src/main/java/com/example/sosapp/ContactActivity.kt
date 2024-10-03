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
        val priorityCheckBox = dialogView.findViewById<CheckBox>(R.id.checkBoxPriority)

        AlertDialog.Builder(this)
            .setTitle("Add Contact")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameEditText.text.toString().trim()
                val phone = phoneEditText.text.toString().trim()
                val isPriority = priorityCheckBox.isChecked

                // Validation for phone number
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    if (phone.length == 10 && phone.all { it.isDigit() }) {
                        if (isPriority) {
                            // Remove priority from all other contacts
                            contactList.forEach { contact ->
                                if (contact.isPriority) {
                                    contact.isPriority = false
                                    updateContactInFirestore(contact) // Update Firestore record
                                }
                            }
                        }
                        val newContact = Contact(name, phone, isPriority)
                        contactList.add(newContact)
                        contactAdapter.notifyDataSetChanged()

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
                "isPriority" to contact.isPriority
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

    private fun updateContactInFirestore(contact: Contact) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            firestore.collection("contacts").document(userId)
                .collection("user_contacts")
                .whereEqualTo("phone", contact.phone) // Assuming phone number is unique
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        firestore.collection("contacts").document(userId)
                            .collection("user_contacts")
                            .document(document.id)
                            .update("isPriority", contact.isPriority)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("ContactActivity", "Error updating contact", e)
                }
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
                    for (document in result) {
                        val contact = document.toObject(Contact::class.java)
                        contactList.add(contact)
                    }
                    contactAdapter.notifyDataSetChanged() // Notify adapter about data changes
                }
                .addOnFailureListener { exception ->
                    Log.w("ContactActivity", "Error getting contacts: ", exception)
                }
        } else {
            Log.w("ContactActivity", "User not authenticated.")
        }
    }
}
