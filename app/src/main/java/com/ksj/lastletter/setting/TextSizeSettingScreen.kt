package com.ksj.lastletter.setting

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

// DataStore 설정 (Context에 확장 속성으로 추가)
val Context.dataStore by preferencesDataStore(name = "settings")
// 저장 키
val TEXT_SIZE_OPTION_KEY = stringPreferencesKey("text_size_option")

suspend fun saveTextSizeOption(context: Context, option: TextSizeOption) {
    context.dataStore.edit { settings ->
        settings[TEXT_SIZE_OPTION_KEY] = option.name
    }
}

suspend fun getTextSizeOption(context: Context): TextSizeOption {
    val preferences = context.dataStore.data.firstOrNull()
    val optionName = preferences?.get(TEXT_SIZE_OPTION_KEY) ?: TextSizeOption.MEDIUM.name
    return TextSizeOption.valueOf(optionName)
}

// 텍스트 크기 옵션 enum (한글 레이블 포함)
enum class TextSizeOption(val label: String) {
    SMALL("작게"),
    MEDIUM("보통"),
    LARGE("크게"),
    EXTRA_LARGE("아주 크게")
}

@Composable
fun TextSizeSettingScreen(
    navController: NavController,
    selectedTextSize: TextSizeOption,
    onTextSizeChange: (TextSizeOption) -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "텍스트 크기 설정",
            fontSize = 20.sp,
            modifier = Modifier.padding(16.dp)
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "글자크기 선택: ${selectedTextSize.label}",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "드롭다운 열기",
                    modifier = Modifier.padding(16.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                TextSizeOption.values().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option.label) },
                        onClick = {
                            onTextSizeChange(option)
                            coroutineScope.launch {
                                saveTextSizeOption(context, option)
                            }
                            expanded = false
                        }
                    )
                }
            }
        }
        Button(
            onClick = { navController.navigate("settings") },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "설정으로 돌아가기")
        }
    }
}
