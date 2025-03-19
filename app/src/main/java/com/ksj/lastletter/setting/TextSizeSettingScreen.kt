package com.ksj.lastletter.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

// 배경 컬러
private val BackgroundColor = Color(0xFFFDFBF4)
// 카드 컬러(연한 초록)
private val CardColor = Color(0xFFE2FFDE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSizeSettingScreen(
    navController: NavController,
    selectedTextSize: TextSizeOption,
    onTextSizeChange: (TextSizeOption) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // 드롭다운 메뉴 펼침 상태
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = BackgroundColor),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "뒤로가기",
                            tint = Color.Black
                        )
                    }
                },
                title = {
                    Text(
                        text = "글자 크기 변경",
                        color = Color.Black
                    )
                }
            )
        },
        containerColor = BackgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 초록색 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = CardColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // "글자 크기" 텍스트
                    Text(
                        text = "글자 크기",
                        color = Color.Black
                    )
                    // 현재 선택된 옵션 + 드롭다운 아이콘
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedTextSize.label,
                            color = Color.Black
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "드롭다운 열기",
                            tint = Color.Black,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                // 드롭다운 메뉴
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    TextSizeOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = option.label, color = Color.Black)
                                    // 현재 선택된 항목이면 체크 아이콘 표시
                                    if (option == selectedTextSize) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onTextSizeChange(option)
                                coroutineScope.launch {
                                    // DataStore에 저장
                                    saveTextSizeOption(navController.context, option)
                                }
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

        }
    }
}
