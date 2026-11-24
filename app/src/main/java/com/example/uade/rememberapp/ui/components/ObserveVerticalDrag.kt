package com.example.uade.rememberapp.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed

/**
 * Sigue un arrastre vertical sin quedárselo: avisa el recorrido acumulado por [onDrag] mientras
 * el dedo se mueve, y el recorrido final por [onRelease] al soltarlo.
 *
 * En los dos, `dy` negativo es hacia arriba e `isVertical` indica si el gesto fue más vertical
 * que horizontal. Si el gesto se cancela (ej. el puntero desaparece), [onRelease] llega con 0.
 *
 * A diferencia de `draggable`, **solo observa**: lo de adentro y lo de afuera (ej. arrastrar un
 * ModalBottomSheet hacia abajo para cerrarlo) siguen recibiendo el gesto como siempre.
 */
@Composable
fun Modifier.observeVerticalDrag(
    onDrag: (dy: Float) -> Unit,
    onRelease: (dy: Float, isVertical: Boolean) -> Unit,
): Modifier {
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnRelease by rememberUpdatedState(onRelease)

    return pointerInput(Unit) {
        awaitEachGesture {
            // Pasada Initial: se ve el evento antes que los hijos, sin quitárselo a nadie.
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var dx = 0f
            var dy = 0f
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null) {
                    currentOnRelease(0f, false)
                    return@awaitEachGesture
                }

                val delta = change.positionChangeIgnoreConsumed()
                dx += delta.x
                dy += delta.y
                val isVertical = kotlin.math.abs(dy) > kotlin.math.abs(dx)

                if (!change.pressed) {
                    currentOnRelease(dy, isVertical)
                    return@awaitEachGesture
                }
                if (isVertical) currentOnDrag(dy)
            }
        }
    }
}
