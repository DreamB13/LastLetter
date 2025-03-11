package com.ksj.lastletter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun YoursContextScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFFFBF5)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "첫째 딸아이",
                    fontSize = 24.sp,
                    color = Color.Black,
                )
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFFFFDCA8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                ContextAddButton()
            }

            Spacer(modifier = Modifier.height(16.dp))

            val cardData = listOf(
                Pair("1월 7일", "창 밖을 바라보다가"),
                Pair("12월 4일", "설거지는 자기 전에"),
                Pair("12월 2일", "햇살이 좋아서"),
                Pair("11월 26일", "너가 어렸을 때"),
                Pair("10월 7일", "바람이 차, 감기 조심")
            )

            cardData.forEach { (date, text) ->
                InfoCard(date, text)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun InfoCard(date: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(
                color = when (date) {
                    "1월 7일" -> Color(0xFFFFF4E6)
                    "12월 4일" -> Color(0xFFFFE6E6)
                    "12월 2일" -> Color(0xFFE6FFE6)
                    "11월 26일" -> Color(0xFFE6E6FF)
                    else -> Color(0xFFFFE6FF)
                },
                shape = RoundedCornerShape(12.dp)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = date,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.padding(start = 16.dp)
        )
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.padding(end = 16.dp)
        )
    }
}

@Composable
fun ContextAddButton() {
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 48.dp)
            .background(Color.White, shape = RoundedCornerShape(12.dp))
            .border(2.dp, Color(0xFFFFDCA8), shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+",
            fontSize = 24.sp,
            color = Color(0xFFFFDCA8)
        )
    }
}
