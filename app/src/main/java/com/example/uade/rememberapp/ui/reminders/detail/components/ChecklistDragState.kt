package com.example.uade.rememberapp.ui.reminders.detail.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Estado del arrastre para reordenar la lista del detalle (la manija ≡ de cada ítem).
 *
 * Cómo funciona:
 * 1. Mantener apretada la manija llama a [start] con el ítem.
 * 2. Mientras se arrastra, [drag] suma cuánto se movió el dedo en [offset]: la fila se dibuja
 *    corrida esa distancia, siguiendo al dedo.
 * 3. Cuando pasó más de la mitad de la fila de al lado, se intercambian de verdad (onMove, que
 *    cambia el orden en el ViewModel). La fila arrastrada ahora está un lugar más allá, así que
 *    al offset se le resta lo que mide la otra fila: en pantalla no salta.
 * 4. Al soltar, [end] la deja en su lugar.
 *
 * Es estado puramente visual (dónde está el dedo), por eso vive en la UI con `remember` y no en
 * el ViewModel; el ViewModel solo se entera del nuevo orden.
 *
 * Los índices son posiciones en la lista completa de ítems (los tildados también cuentan, aunque
 * no se puedan arrastrar).
 */
@Stable
class ChecklistDragState {
    /** El ítem que se está arrastrando, o null. */
    var draggedIndex by mutableStateOf<Int?>(null)
        private set

    /** Cuánto se corrió del lugar que le toca, en px (negativo = hacia arriba). */
    var offset by mutableFloatStateOf(0f)
        private set

    /** Alto de cada fila activa por posición en pantalla (no es estado: no se dibuja). */
    val rowHeights = mutableMapOf<Int, Int>()

    /** El espacio entre filas, en px: también hay que saltarlo al intercambiar. */
    var spacingPx = 0f

    // Después de un intercambio hay que esperar a que lleguen los ítems con el orden nuevo: si
    // no, el próximo movimiento del dedo calcularía con la lista vieja y movería dos veces.
    private var latestItems: Any? = null
    private var itemsAtLastMove: Any? = null
    private var waitingForUpdate = false

    fun start(index: Int) {
        draggedIndex = index
        offset = 0f
    }

    /** Llega en cada recomposición con la lista de ítems actual. */
    fun onItemsUpdated(items: Any) {
        latestItems = items
        if (items !== itemsAtLastMove) waitingForUpdate = false
    }

    /**
     * El dedo se movió [dy] px. [activeIndices] son los ítems arrastrables en el orden en que se
     * ven; [onMove] cambia de lugar dos ítems (from, to) en el ViewModel.
     */
    fun drag(dy: Float, activeIndices: List<Int>, onMove: (from: Int, to: Int) -> Unit) {
        val from = draggedIndex ?: return
        offset += dy
        if (waitingForUpdate) return
        val position = activeIndices.indexOf(from)
        if (position < 0) return

        val neighbor = when {
            offset > 0 && position < activeIndices.lastIndex -> position + 1
            offset < 0 && position > 0 -> position - 1
            else -> return
        }
        val neighborHeight = rowHeights[neighbor] ?: return
        if (kotlin.math.abs(offset) < neighborHeight / 2f) return

        val to = activeIndices[neighbor]
        onMove(from, to)
        // Sacarlo de `from` y ponerlo en `to` lo deja justo en `to`.
        draggedIndex = to
        val jump = neighborHeight + spacingPx
        offset += if (neighbor > position) -jump else jump
        itemsAtLastMove = latestItems
        waitingForUpdate = true
    }

    fun end() {
        draggedIndex = null
        offset = 0f
        waitingForUpdate = false
    }
}
