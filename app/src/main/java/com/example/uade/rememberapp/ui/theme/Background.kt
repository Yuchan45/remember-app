package com.example.uade.rememberapp.ui.theme

import androidx.compose.foundation.background
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush

val LocalUseGradientBackground = compositionLocalOf { true }

/**
 * Fondo de las pantallas principales: degradado sutil adaptado a la paleta elegida o color
 * plano según la preferencia del usuario configurada en Ajustes -> Apariencia.
 */
fun Modifier.appBackground(): Modifier = composed {
    val useGradient = LocalUseGradientBackground.current
    val backgroundColors = LocalAppBackgroundColors.current

    if (useGradient) {
        val brush = Brush.verticalGradient(listOf(backgroundColors.top, backgroundColors.bottom))
        background(brush)
    } else {
        background(backgroundColors.bottom)
    }
}
