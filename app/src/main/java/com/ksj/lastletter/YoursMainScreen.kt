package com.ksj.lastletter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp



@Preview
@Composable
fun MainScreen() {
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = Color(0xFFFFFBF5)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "남은 너에게",
                fontSize = 20.sp,
                color = Color.Black,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(24.dp))

            InfoCard(text = "남편")

            Spacer(modifier = Modifier.height(16.dp))

            InfoCard("첫째 딸아이")

            Spacer(modifier = Modifier.height(32.dp))

            AddButton()

        }
    }
}

@Composable
fun InfoCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(
                color = Color(0xFFFFF4E6),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun AddButton() {
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 48.dp) // 라운드 직사각형 크기 설정
            .background(Color.White, shape = RoundedCornerShape(12.dp)) // 흰색 배경과 둥근 모서리 설정
            .border(2.dp, Color(0xFFFFDCA8), shape = RoundedCornerShape(12.dp)), // 테두리 색상 설정
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+",
            fontSize = 24.sp,
            color = Color(0xFFFFDCA8) // "+" 색상 설정
        )
    }
}