package com.ksj.lastletter.setting

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

// 1. DataStore 설정
val Context.dataStore by preferencesDataStore(name = "settings")
val TEXT_SIZE_OPTION_KEY = stringPreferencesKey("text_size_option")

// 2. 텍스트 사이즈 옵션
enum class TextSizeOption(val label: String) {
    VERY_SMALL("아주 작게"),
    SMALL("작게"),
    MEDIUM("보통"),
    LARGE("크게"),
    VERY_LARGE("매우 크게")
}

// 3. 저장 함수
suspend fun saveTextSizeOption(context: Context, option: TextSizeOption) {
    context.dataStore.edit { settings ->
        settings[TEXT_SIZE_OPTION_KEY] = option.name
    }
}

// 4. 불러오기 함수
suspend fun getTextSizeOption(context: Context): TextSizeOption {
    val preferences = context.dataStore.data.firstOrNull()
    val optionName = preferences?.get(TEXT_SIZE_OPTION_KEY) ?: TextSizeOption.MEDIUM.name

    // enum 매칭 시도 → 없으면 MEDIUM으로
    return TextSizeOption.entries.find { it.name == optionName } ?: TextSizeOption.MEDIUM
}
