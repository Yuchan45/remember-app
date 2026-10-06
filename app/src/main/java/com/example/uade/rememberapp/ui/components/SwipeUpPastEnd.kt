package com.example.uade.rememberapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

/**
 * Avisa por [onSwipeUp] cuando el usuario sigue deslizando hacia arriba y la lista de adentro
 * ya no puede subir más: porque entra entera en la pantalla o porque llegó al final.
 *
 * Mientras la lista pueda hacer scroll, el gesto es un scroll normal y no dispara nada. Tiene
 * que sobrar al menos [threshold] de recorrido en un mismo gesto; un toque o un roce no alcanza.
 * Se aplica sobre el contenedor de la lista (o sobre la lista misma).
 */
@Composable
fun Modifier.swipeUpPastEnd(
    onSwipeUp: () -> Unit,
    threshold: Dp = 48.dp,
): Modifier {
    val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
    val currentOnSwipeUp by rememberUpdatedState(onSwipeUp)
    val connection = remember(thresholdPx) {
        SwipeUpPastEndConnection(thresholdPx) { currentOnSwipeUp() }
    }
    return nestedScroll(connection)
}

private class SwipeUpPastEndConnection(
    private val thresholdPx: Float,
    private val onTrigger: () -> Unit,
) : NestedScrollConnection {

    /** Recorrido hacia arriba que la lista no pudo usar, en el gesto actual. */
    private var accumulated = 0f

    /** Para disparar una sola vez por gesto, aunque el dedo siga subiendo. */
    private var triggered = false

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        // Solo cuenta el dedo arrastrando, no la inercia de un fling.
        if (source != NestedScrollSource.UserInput) return Offset.Zero

        when {
            // Dedo hacia arriba (y negativo) que la lista no consumió: está al final o no scrollea.
            available.y < 0f -> {
                accumulated -= available.y
                if (!triggered && accumulated >= thresholdPx) {
                    triggered = true
                    onTrigger()
                }
            }
            // Si vuelve a scrollear o baja, empieza de cero.
            consumed.y != 0f || available.y > 0f -> accumulated = 0f
        }
        // No consume nada: el efecto de estiramiento del borde se sigue viendo.
        return Offset.Zero
    }

    /** El dedo se levantó: termina el gesto. */
    override suspend fun onPreFling(available: Velocity): Velocity {
        accumulated = 0f
        triggered = false
        return Velocity.Zero
    }
}
