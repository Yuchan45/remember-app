package com.example.uade.rememberapp.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Forma ondulada tipo "cookie" de Material 3 Expressive (como la "Sunny"): un círculo con
 * [CookieLobes] lóbulos suaves.
 *
 * Material3 trae estas formas en `MaterialShapes`, pero recién en versiones posteriores a la
 * 1.4.0 que usa el proyecto; para no actualizar dependencias se arma con la misma idea:
 * un radio que oscila con un coseno, `r = radio + onda · cos(lóbulos · t)`.
 *
 * Los puntos se calculan una sola vez (en una caja de 0 a 1) y solo se escalan al tamaño.
 */
val CookieShape: Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        CookiePoints.forEachIndexed { index, point ->
            val x = point.x * size.width
            val y = point.y * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

private const val CookieLobes = 8
private const val CookieSteps = 240

/** Radio base y amplitud de la onda, relativos a la caja (0,5 = medio ancho). */
private const val CookieRadius = 0.45f
private const val CookieWave = 0.032f

/** Contorno precalculado, en coordenadas de 0 a 1 (centro en 0,5; 0,5). */
private val CookiePoints: List<Offset> = List(CookieSteps + 1) { i ->
    val t = i.toDouble() / CookieSteps * 2 * PI
    val r = CookieRadius + CookieWave * cos(CookieLobes * t).toFloat()
    Offset(
        x = 0.5f + r * cos(t).toFloat(),
        y = 0.5f + r * sin(t).toFloat(),
    )
}
