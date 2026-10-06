package com.example.uade.rememberapp.data.repository

import com.example.uade.rememberapp.domain.model.Tag
import com.example.uade.rememberapp.domain.repository.InvalidTagNameException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class InMemoryTagRepositoryTest {

    private val salud = Tag(id = 1, name = "Salud", colorArgb = 0xFF6FCF97)

    @Test
    fun `create agrega la etiqueta con un id nuevo y el nombre sin espacios de más`() = runBlocking {
        val repository = InMemoryTagRepository(initial = listOf(salud))

        val created = repository.create("  Facultad ", 0xFF4EE6E6)

        assertEquals(2L, created.id)
        assertEquals("Facultad", created.name)
        assertEquals(listOf(salud, created), repository.observeAll().first())
    }

    @Test
    fun `create rechaza nombre vacío, demasiado largo o repetido`() = runBlocking {
        val repository = InMemoryTagRepository(initial = listOf(salud))

        assertInvalid(InvalidTagNameException.Reason.Empty) { repository.create("   ", 0) }
        assertInvalid(InvalidTagNameException.Reason.TooLong) { repository.create("a".repeat(21), 0) }
        assertInvalid(InvalidTagNameException.Reason.Duplicate) { repository.create("salud", 0) }
        assertEquals(listOf(salud), repository.observeAll().first())
    }

    @Test
    fun `update cambia nombre y color, y permite dejar el mismo nombre`() = runBlocking {
        val repository = InMemoryTagRepository(initial = listOf(salud))

        repository.update(salud.copy(colorArgb = 0xFF000000))
        repository.update(salud.copy(name = "Médico"))

        assertEquals("Médico", repository.observeAll().first().single().name)
    }

    @Test
    fun `delete la quita y restore la devuelve con el mismo id`() = runBlocking {
        val repository = InMemoryTagRepository(initial = listOf(salud))

        repository.delete(salud.id)
        assertTrue(repository.observeAll().first().isEmpty())

        repository.restore(salud)
        assertEquals(listOf(salud), repository.observeAll().first())
    }

    private suspend fun assertInvalid(expected: InvalidTagNameException.Reason, block: suspend () -> Unit) {
        try {
            block()
            fail("Se esperaba InvalidTagNameException($expected)")
        } catch (e: InvalidTagNameException) {
            assertEquals(expected, e.reason)
        }
    }
}
