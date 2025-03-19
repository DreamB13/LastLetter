package com.ksj.lastletter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.ksj.lastletter.setting.TextSizeOption

private val DarkColorScheme = darkColorScheme(
    // 필요 시 색상 지정
    // e.g. primary = Color(0xFF6200EE)
)

private val LightColorScheme = lightColorScheme(
    background = Color(0xFFFDFBF4)
    // 필요 시 색상 지정
)

@Composable
fun LastLetterTheme(
    textSizeOption: TextSizeOption,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // 선택된 옵션에 따른 Typography를 정의
    val typography = when (textSizeOption) {
        TextSizeOption.VERY_SMALL -> Typography(
            bodyLarge = TextStyle(fontSize = 12.sp),
            bodyMedium = TextStyle(fontSize = 12.sp),
            bodySmall = TextStyle(fontSize = 12.sp)
        )
        TextSizeOption.SMALL -> Typography(
            bodyLarge = TextStyle(fontSize = 14.sp),
            bodyMedium = TextStyle(fontSize = 14.sp),
            bodySmall = TextStyle(fontSize = 14.sp)
        )
        TextSizeOption.MEDIUM -> Typography(
            bodyLarge = TextStyle(fontSize = 16.sp),
            bodyMedium = TextStyle(fontSize = 16.sp),
            bodySmall = TextStyle(fontSize = 16.sp)
        )
        TextSizeOption.LARGE -> Typography(
            bodyLarge = TextStyle(fontSize = 20.sp),
            bodyMedium = TextStyle(fontSize = 20.sp),
            bodySmall = TextStyle(fontSize = 20.sp)
        )
        TextSizeOption.VERY_LARGE -> Typography(
            bodyLarge = TextStyle(fontSize = 24.sp),
            bodyMedium = TextStyle(fontSize = 24.sp),
            bodySmall = TextStyle(fontSize = 24.sp)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
