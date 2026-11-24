package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.CropRect
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.repository.PhotoRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class DeleteRemindersForeverUseCaseTest {

    private val repository = FakeReminderRepository()
    private val files = object : PhotoRepository {
        val deleted = mutableListOf<String>()
        override suspend fun import(uri: String): String = uri
        override suspend fun saveCover(sourcePath: String, crop: CropRect): String = sourcePath
        override suspend fun delete(path: String) {
            deleted += path
        }
    }
    private val deleteForever = DeleteRemindersForeverUseCase(repository, FakeReminderScheduler(), files)

    private suspend fun fromAudio(title: String) = repository.save(
        Reminder(title = title, audioPath = "/audios/nota.m4a", transcript = "…", createdAt = Instant.EPOCH),
    )

    @Test
    fun `no borra la grabacion si otro recordatorio del mismo audio la sigue usando`() = runBlocking {
        val first = fromAudio("Ir al supermercado")
        fromAudio("Llamar al médico")

        deleteForever(listOf(first))

        assertEquals(emptyList<String>(), files.deleted)
    }

    @Test
    fun `borra la grabacion cuando se borra el ultimo que la usaba`() = runBlocking {
        val first = fromAudio("Ir al supermercado")
        val second = fromAudio("Llamar al médico")

        deleteForever(listOf(first, second))

        assertEquals(listOf("/audios/nota.m4a"), files.deleted)
    }
}
