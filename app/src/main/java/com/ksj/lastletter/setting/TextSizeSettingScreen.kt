package com.ksj.lastletter.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// 텍스트 크기 옵션 enum (필요에 따라 별도 공통 파일로 관리 가능)
enum class TextSizeOption {
    SMALL, MEDIUM, LARGE, EXTRA_LARGE
}

@Composable
fun TextSizeSettingScreen(
    navController: NavController,
    selectedTextSize: TextSizeOption,
    onTextSizeChange: (TextSizeOption) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "텍스트 크기 설정",
            fontSize = 20.sp,
            modifier = Modifier.padding(16.dp)
        )
        Button(
            onClick = { onTextSizeChange(TextSizeOption.SMALL) },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "작게")
        }
        Button(
            onClick = { onTextSizeChange(TextSizeOption.MEDIUM) },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "보통")
        }
        Button(
            onClick = { onTextSizeChange(TextSizeOption.LARGE) },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "크게")
        }
        Button(
            onClick = { onTextSizeChange(TextSizeOption.EXTRA_LARGE) },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "아주 크게")
        }
        Button(
            onClick = { navController.navigate("settings") },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "설정으로 돌아가기")
        }
    }
}
