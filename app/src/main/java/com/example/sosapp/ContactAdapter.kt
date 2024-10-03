package com.example.sosapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactAdapter(private val contactList: List<Contact>) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    // ViewHolder to bind the layout views
    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactName: TextView = itemView.findViewById(R.id.contactName)
        val contactPhone: TextView = itemView.findViewById(R.id.contactPhone)
        val priorityIndicator: TextView = itemView.findViewById(R.id.priorityIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        // Inflate the contact item layout
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contactList[position]

        // Bind the data to the TextViews
        holder.contactName.text = contact.name
        holder.contactPhone.text = contact.phone

        // Set the priority indicator visibility based on priority status
        if (contact.isPriority) {
            holder.priorityIndicator.text = "Priority Contact" // You can change the text or style
            holder.priorityIndicator.visibility = View.VISIBLE
        } else {
            holder.priorityIndicator.visibility = View.GONE
        }
    }


    override fun getItemCount(): Int {
        return contactList.size
    }
}
