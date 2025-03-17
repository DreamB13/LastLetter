package com.ksj.lastletter

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        val db = FirebaseFirestore.getInstance()
        // 오프라인 퍼시스턴스 활성화: 이미 데이터를 캐시해두므로 네트워크 호출이 줄어듭니다.
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        db.firestoreSettings = settings
    }
}
