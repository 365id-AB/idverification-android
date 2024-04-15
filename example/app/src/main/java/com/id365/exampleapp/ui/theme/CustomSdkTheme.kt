package com.id365.exampleapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomSdkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit)
{
    val customColors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xff702f9a),
            onPrimary = Color(0xffffffff),
            secondary = Color(0xffdcdcdc),
            onSecondary = Color(0xff707070),
            error = Color(0xFFFF0000),
            onError = Color(0xffffffff),
            background = Color(0xffffffff),
            onBackground = Color(0xff333333),
            surface = Color(0xfff5f5f5),
            onSurface = Color(0xff702f9a)
        )
    } else {
        darkColorScheme(
            primary = Color(0xff702f9a),
            onPrimary = Color(0xffffffff),
            secondary = Color(0xffdcdcdc),
            onSecondary = Color(0xff707070),
            error = Color(0xFFFF0000),
            onError = Color(0xffffffff),
            background = Color(0xffffffff),
            onBackground = Color(0xff333333),
            surface = Color(0xfff5f5f5),
            onSurface = Color(0xff702f9a)
        )
    }

    val customTypography = Typography(
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Light,
            fontSize = 14.sp,
            lineHeight = 24.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        ),
        titleSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Light,
            fontSize = 14.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Light,
            fontSize = 14.sp
        )
    )

    val customShapes = Shapes(
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(0.dp)
    )

    MaterialTheme(
        colorScheme = customColors,
        typography = customTypography,
        shapes = customShapes,
        content = content
    )
}
