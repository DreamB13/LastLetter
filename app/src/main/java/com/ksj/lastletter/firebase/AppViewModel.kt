package com.ksj.lastletter

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ksj.lastletter.firebase.ContactRepository
import com.ksj.lastletter.firebase.DocumentContact
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val contactRepository = ContactRepository()

    // 연락처 목록을 전역 상태로 보관 (앱 시작 시 한 번만 로드)
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

    // 필요시 refreshContacts()를 호출하여 다시 로드할 수 있습니다.
    fun refreshContacts() {
        loadContacts()
    }
}
