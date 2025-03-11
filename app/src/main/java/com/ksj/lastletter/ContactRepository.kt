package com.ksj.lastletter

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject

class ContactRepository {
    private val db = FirebaseFirestore.getInstance()
    private val contactsRef = db.collection("contacts")

    fun addContact(contact: Contact) {
        contactsRef.add(contact)
            .addOnSuccessListener { documentReference ->
                println("DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                println("Error adding document: $e")
            }
    }

    fun getContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        db.collection("contacts").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                task.result.forEach { document ->
                    val contact = document.toObject<Contact>()
                    contacts.add(contact)
                }
            }
        }
        return contacts
    }
}
