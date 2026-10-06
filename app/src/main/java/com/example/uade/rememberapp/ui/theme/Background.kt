package com.example.uade.rememberapp.ui.theme

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

private val AppBackgroundBrush = Brush.verticalGradient(listOf(BackgroundTop, Background))

/**
 * Fondo en degradé de las pantallas principales. Cada pantalla lo aplica sobre su raíz (con
 * el Scaffold en `containerColor = Color.Transparent`), así también se ve en sus previews.
 */
fun Modifier.appBackground(): Modifier = background(AppBackgroundBrush)
