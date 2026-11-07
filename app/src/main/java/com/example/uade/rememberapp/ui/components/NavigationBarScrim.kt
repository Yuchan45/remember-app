package com.example.uade.rememberapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Velo negro detrás de la barra de navegación del sistema (la de los botones o el gesto de
 * Android). La app dibuja de borde a borde, así que esa barra es transparente y por defecto
 * deja ver el contenido de atrás: con esto queda negra pero translúcida, más oscura abajo.
 *
 * Ocupa justo el alto de la barra del sistema. Arriba empalma con [NavigationBarFadeColor], el
 * tono en que termina el degradé de la barra inferior de la app (ver MainTabs), para que se vea
 * un solo degradé continuo. Va encima de todo, pero no recibe toques: los deja pasar.
 */
@Composable
fun NavigationBarScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsBottomHeight(WindowInsets.navigationBars)
            .background(Brush.verticalGradient(listOf(NavigationBarFadeColor, Color.Black.copy(alpha = 0.9f)))),
    )
}

/**
 * El negro translúcido en el que termina el degradé de arriba de la barra del sistema. Lo comparten
 * [NavigationBarScrim] y el fondo de la barra inferior de MainTabs para que empalmen.
 */
val NavigationBarFadeColor = Color.Black.copy(alpha = 0.6f)

