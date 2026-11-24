package com.example.uade.rememberapp.ui.reminders.detail.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChecklistDragStateTest {

    /** Una lista de ítems en memoria y el estado de arrastre conectado a ella. */
    private class Harness(size: Int, rowHeight: Int = 100) {
        var items = (0 until size).toList()
        val moves = mutableListOf<Pair<Int, Int>>()
        val state = ChecklistDragState().apply {
            spacingPx = 10f
            (0 until size).forEach { rowHeights[it] = rowHeight }
            onItemsUpdated(items)
        }

        fun drag(dy: Float) = state.drag(dy, items.indices.toList()) { from, to ->
            moves += from to to
            items = items.toMutableList().apply { add(to, removeAt(from)) }
        }

        /** Lo que hace la recomposición: avisar que llegaron los ítems con el orden nuevo. */
        fun recompose() = state.onItemsUpdated(items)
    }

    @Test
    fun `menos de media fila no mueve nada`() {
        val h = Harness(size = 3)
        h.state.start(0)

        h.drag(49f)

        assertEquals(emptyList<Pair<Int, Int>>(), h.moves)
        assertEquals(49f, h.state.offset)
    }

    @Test
    fun `pasar media fila intercambia con el de abajo y corrige el offset`() {
        val h = Harness(size = 3)
        h.state.start(0)

        h.drag(60f)

        assertEquals(listOf(0 to 1), h.moves)
        assertEquals(1, h.state.draggedIndex)
        // Bajó un lugar (100 de fila + 10 de separación): quedan 60 - 110 = -50 respecto del nuevo.
        assertEquals(-50f, h.state.offset)
    }

    @Test
    fun `no mueve dos veces antes de que lleguen los items nuevos`() {
        val h = Harness(size = 4)
        h.state.start(0)

        h.drag(60f)
        h.drag(200f) // Sin recomponer todavía: solo acumula.
        assertEquals(listOf(0 to 1), h.moves)

        h.recompose()
        h.drag(0f)
        assertEquals(listOf(0 to 1, 1 to 2), h.moves)
    }

    @Test
    fun `arrastrar hacia arriba intercambia con el de arriba`() {
        val h = Harness(size = 3)
        h.state.start(2)

        h.drag(-70f)

        assertEquals(listOf(2 to 1), h.moves)
        assertEquals(40f, h.state.offset)
    }

    @Test
    fun `soltar deja todo en su lugar`() {
        val h = Harness(size = 3)
        h.state.start(1)
        h.drag(30f)

        h.state.end()

        assertNull(h.state.draggedIndex)
        assertEquals(0f, h.state.offset)
    }
}
