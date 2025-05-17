package com.example.sosapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sosapp.databinding.ActivityContactBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class ContactActivity : AppCompatActivity(), ContactAdapter.OnContactClickListener {
    private val contactList = ArrayList<Contact>()
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var binding: ActivityContactBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        binding.recyclerViewContacts.layoutManager = LinearLayoutManager(this)
        contactAdapter = ContactAdapter(contactList, this)
        binding.recyclerViewContacts.adapter = contactAdapter

        loadContacts()

        binding.addcontact.setOnClickListener {
            showAddOrEditDialog(null, null)
        }

        binding.homebtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun showAddOrEditDialog(contact: Contact?, docId: String?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.editTextName)
        val phoneEditText = dialogView.findViewById<EditText>(R.id.editTextPhone)

        if (contact != null) {
            nameEditText.setText(contact.name)
            phoneEditText.setText(contact.phone)
        }

        AlertDialog.Builder(this)
            .setTitle(if (contact == null) "Add Contact" else "Edit Contact")
            .setView(dialogView)
            .setPositiveButton(if (contact == null) "Add" else "Update") { _, _ ->
                val name = nameEditText.text.toString().trim()
                val phone = phoneEditText.text.toString().trim()

                if (name.isNotEmpty() && phone.length == 10 && phone.all { it.isDigit() }) {
                    val newContact = Contact(name, phone)

                    if (docId == null) {
                        addContact(newContact)
                    } else {
                        updateContact(newContact, docId)
                    }
                } else {
                    Toast.makeText(this, "Enter valid name and 10-digit phone number", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addContact(contact: Contact) {
        firestore.collection("contacts").document(userId).collection("user_contacts")
            .add(contact)
            .addOnSuccessListener {
                loadContacts()
                Toast.makeText(this, "Contact added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add contact", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateContact(contact: Contact, docId: String) {
        firestore.collection("contacts").document(userId)
            .collection("user_contacts").document(docId)
            .set(contact)
            .addOnSuccessListener {
                loadContacts()
                Toast.makeText(this, "Contact updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update contact", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteContact(docId: String) {
        firestore.collection("contacts").document(userId)
            .collection("user_contacts").document(docId)
            .delete()
            .addOnSuccessListener {
                loadContacts()
                Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete contact", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadContacts() {
        firestore.collection("contacts").document(userId).collection("user_contacts")
            .get()
            .addOnSuccessListener { result ->
                contactList.clear()
                for (doc in result) {
                    val contact = doc.toObject(Contact::class.java)
                    contact.docId = doc.id
                    contactList.add(contact)
                }
                contactAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load contacts", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onEditClicked(contact: Contact) {
        showAddOrEditDialog(contact, contact.docId)
    }

    override fun onDeleteClicked(contact: Contact) {
        AlertDialog.Builder(this)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete this contact?")
            .setPositiveButton("Delete") { _, _ -> deleteContact(contact.docId ?: "") }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
