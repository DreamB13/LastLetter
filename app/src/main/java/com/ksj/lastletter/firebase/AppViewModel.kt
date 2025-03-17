package com.ksj.lastletter

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ksj.lastletter.firebase.ContactRepository
import com.ksj.lastletter.firebase.DocumentContact
import kotlinx.coroutines.launch

/**
 * 앱 전역에서 한 번만 Firebase 데이터를 로드하여 캐시로 유지하는 ViewModel
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val contactRepository = ContactRepository()

    // 연락처 목록을 앱 전역에서 캐시 (변경이 없다면 다시 로드하지 않음)
    var contacts = mutableStateOf<List<DocumentContact>>(emptyList())
        private set

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            contacts.value = contactRepository.getContactsWithCoroutines()
        }
    }

    // 데이터 변경이 발생하면 필요에 따라 새로고침할 수 있는 함수
    fun refreshContacts() {
        loadContacts()
    }
}
