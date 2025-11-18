package com.example.nousappathon.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.nousappathon.R

val CaveatFamily = FontFamily(
    Font(R.font.caveat_regular, FontWeight.Normal),
    Font(R.font.caveat_bold, FontWeight.Bold)
)

val AppTypography = Typography(
    displayLarge = TextStyle( // choose styles you want to override
        fontFamily = CaveatFamily,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold
    ),
    bodyLarge = TextStyle(
        fontFamily = CaveatFamily,
        fontSize = 16.sp
    )
    // add other text styles you want to customize...
)
