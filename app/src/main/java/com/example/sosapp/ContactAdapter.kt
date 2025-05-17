package com.example.sosapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactAdapter(
    private val contactList: List<Contact>,
    private val listener: OnContactClickListener
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    interface OnContactClickListener {
        fun onEditClicked(contact: Contact)
        fun onDeleteClicked(contact: Contact)
    }

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactName: TextView = itemView.findViewById(R.id.contactName)
        val contactPhone: TextView = itemView.findViewById(R.id.contactPhone)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contactList[position]
        holder.contactName.text = contact.name
        holder.contactPhone.text = contact.phone

        holder.editButton.setOnClickListener {
            listener.onEditClicked(contact)
        }

        holder.deleteButton.setOnClickListener {
            listener.onDeleteClicked(contact)
        }
    }

    override fun getItemCount(): Int = contactList.size
}
