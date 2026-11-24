package com.example.uade.rememberapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.AppFontFamily
import com.example.uade.rememberapp.domain.model.FontScale

val AtkinsonHyperlegibleFamily = FontFamily(
    Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_bold, FontWeight.Bold),
)

fun resolveFontFamily(family: AppFontFamily): FontFamily = when (family) {
    AppFontFamily.System -> FontFamily.Default
    AppFontFamily.AtkinsonHyperlegible -> AtkinsonHyperlegibleFamily
    AppFontFamily.SansSerif -> FontFamily.SansSerif
    AppFontFamily.Serif -> FontFamily.Serif
    AppFontFamily.Monospace -> FontFamily.Monospace
}

/**
 * Genera la tipografía de Material3 adaptada a la escala de texto y a la familia de fuentes elegida.
 */
fun createAppTypography(
    fontFamily: AppFontFamily = AppFontFamily.System,
    fontScale: FontScale = FontScale.Default,
): Typography {
    val family = resolveFontFamily(fontFamily)
    val factor = fontScale.factor

    return Typography(
        displayLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (57 * factor).sp,
            lineHeight = (64 * factor).sp,
            letterSpacing = (-0.25).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (45 * factor).sp,
            lineHeight = (52 * factor).sp,
            letterSpacing = 0.sp,
        ),
        displaySmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (36 * factor).sp,
            lineHeight = (44 * factor).sp,
            letterSpacing = 0.sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = (32 * factor).sp,
            lineHeight = (40 * factor).sp,
            letterSpacing = 0.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = (28 * factor).sp,
            lineHeight = (36 * factor).sp,
            letterSpacing = 0.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = (24 * factor).sp,
            lineHeight = (32 * factor).sp,
            letterSpacing = 0.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (22 * factor).sp,
            lineHeight = (28 * factor).sp,
            letterSpacing = 0.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (16 * factor).sp,
            lineHeight = (24 * factor).sp,
            letterSpacing = 0.15.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (14 * factor).sp,
            lineHeight = (20 * factor).sp,
            letterSpacing = 0.1.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * factor).sp,
            lineHeight = (24 * factor).sp,
            letterSpacing = 0.5.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * factor).sp,
            lineHeight = (20 * factor).sp,
            letterSpacing = 0.25.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (12 * factor).sp,
            lineHeight = (16 * factor).sp,
            letterSpacing = 0.4.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (14 * factor).sp,
            lineHeight = (20 * factor).sp,
            letterSpacing = 0.1.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (12 * factor).sp,
            lineHeight = (16 * factor).sp,
            letterSpacing = 0.5.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (11 * factor).sp,
            lineHeight = (16 * factor).sp,
            letterSpacing = 0.5.sp,
        ),
    )
}

/** Tipografía por defecto para previews. */
val Typography = createAppTypography()
