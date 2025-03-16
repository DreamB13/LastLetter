package com.ksj.lastletter.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ContactRepository {
    private val db = FirebaseFirestore.getInstance()

    // 사용자 uid를 기준으로 "Yours" 컬렉션 참조
    private fun getContactsRef(): CollectionReference? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return db.collection("users").document(uid).collection("Yours")
    }

    fun addContact(contact: Contact) {
        getContactsRef()?.add(contact)
            ?.addOnSuccessListener { documentReference ->
                println("DocumentSnapshot added with ID: ${documentReference.id}")
            }
            ?.addOnFailureListener { e ->
                println("Error adding document: $e")
            }
    }

    // 코루틴을 사용한 데이터 로드
    suspend fun getContactsWithCoroutines(): List<DocumentContact> = withContext(Dispatchers.IO) {
        val ref = getContactsRef() ?: return@withContext emptyList<DocumentContact>()
        try {
            val querySnapshot = ref.get().await()
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
            val ref = getContactsRef() ?: return@withContext null
            val document = ref.document(documentId).get().await()
            document.toObject(Contact::class.java)
        } catch (e: Exception) {
            println("Error getting contact by ID: ${e.message}")
            null
        }
    }

    suspend fun updateContact(documentId: String, contact: Contact): Boolean = withContext(Dispatchers.IO) {
        try {
            val ref = getContactsRef() ?: return@withContext false
            ref.document(documentId).set(contact).await()
            println("연락처 업데이트 성공: $documentId")
            true
        } catch (e: Exception) {
            println("연락처 업데이트 실패: ${e.message}")
            false
        }
    }

    suspend fun deleteContact(documentId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val ref = getContactsRef() ?: return@withContext false
            ref.document(documentId).delete().await()
            println("연락처 삭제 성공: $documentId")
            true
        } catch (e: Exception) {
            println("연락처 삭제 실패: ${e.message}")
            false
        }
    }

    // 일일질문 받을 사람 설정 – 사용자별로 "DailyQuestionConfig/selectedContacts"에 저장
    fun updateDailyQuestionContacts(newSelectedIds: List<String>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dailyQuestionRef = db.collection("users").document(uid)
            .collection("DailyQuestionConfig").document("selectedContacts")
        dailyQuestionRef.set(mapOf("selectedIds" to newSelectedIds))
            .addOnSuccessListener {
                println("Daily question contacts updated successfully.")
            }
            .addOnFailureListener { e ->
                println("Error updating daily question contacts: $e")
            }
    }

    fun getDailyQuestionContactIds(): List<String> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
        val dailyQuestionRef = db.collection("users").document(uid)
            .collection("DailyQuestionConfig").document("selectedContacts")
        var selectedIds: List<String> = emptyList()
        dailyQuestionRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    selectedIds = document.get("selectedIds") as? List<String> ?: emptyList()
                } else {
                    println("No such document")
                }
            }
            .addOnFailureListener { e ->
                println("Error getting document: $e")
            }
        return selectedIds
    }
}

