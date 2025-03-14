package com.ksj.lastletter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.ksj.lastletter.setting.TextSizeOption

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)
// 기본 라이트 컬러 스킴 (필요에 따라 커스터마이징)
private val LightColors = lightColorScheme(
    // 예: primary = Color(0xFF6200EE), secondary = Color(0xFF03DAC6) 등
)

@Composable
fun LastLetterTheme(
    textSizeOption: TextSizeOption,
    content: @Composable () -> Unit
) {
    // 선택된 옵션에 따른 Typography 정의 (필요에 따라 다른 스타일도 추가)
    val typography = when (textSizeOption) {
        TextSizeOption.SMALL -> Typography(
            bodyLarge = TextStyle(fontSize = 12.sp),
            bodyMedium = TextStyle(fontSize = 12.sp),
            bodySmall = TextStyle(fontSize = 12.sp)
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
        TextSizeOption.EXTRA_LARGE -> Typography(
            bodyLarge = TextStyle(fontSize = 24.sp),
            bodyMedium = TextStyle(fontSize = 24.sp),
            bodySmall = TextStyle(fontSize = 24.sp)
        )
    }

    MaterialTheme(
        typography = typography,
        content = content
    )
}