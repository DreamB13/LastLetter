package com.ksj.lastletter

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore

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

    fun getContacts(): List<Contact> {
        val yours = mutableListOf<Contact>()
        // 비동기 작업이 완료될 때까지 대기하는 방법을 사용
        val task = db.collection("Yours").get()

        // 이 부분은 간단한 해결책으로, 실제로는 suspend 함수와 coroutine을 사용하는 것이 더 좋습니다
        try {
            val documents = Tasks.await(task)
            for (document in documents) {
                val contact = document.toObject(Contact::class.java)
                yours.add(contact)
            }
        } catch (e: Exception) {
            println("Error getting contacts: ${e.message}")
        }

        return yours
    }

}
