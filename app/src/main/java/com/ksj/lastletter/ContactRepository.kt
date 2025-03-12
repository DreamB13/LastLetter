package com.ksj.lastletter

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ContactRepository {
    private val db = FirebaseFirestore.getInstance()
    private val contactsRef = db.collection("Yours")

    fun addContact(contact: Contact) {
        contactsRef.add(contact)
            .addOnSuccessListener { documentReference ->
                println("DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                println("Error adding document: $e")
            }
    }

    // 코루틴을 사용한 데이터 로드 메서드 추가
    suspend fun getContactsWithCoroutines(): List<DocumentContact> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = contactsRef.get().await()
            val documentContacts = mutableListOf<DocumentContact>()
            for (document in querySnapshot.documents) {
                try {
                    val contact = document.toObject(Contact::class.java)
                    if (contact != null) {
                        documentContacts.add(DocumentContact(document.id, contact))
                    }
                } catch (e: Exception) {
                    println("Error converting document: ${e.message}")
                }
            }
            documentContacts
        } catch (e: Exception) {
            println("Error getting contacts: ${e.message}")
            emptyList()
        }
    }

    suspend fun getContactById(documentId: String): Contact? = withContext(Dispatchers.IO) {
        try {
            val document = contactsRef.document(documentId).get().await()
            document.toObject(Contact::class.java)
        } catch (e: Exception) {
            println("Error getting contact by ID: ${e.message}")
            null
        }
    }
}

