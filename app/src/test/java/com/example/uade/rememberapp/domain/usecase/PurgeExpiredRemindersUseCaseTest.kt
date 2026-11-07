package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.CropRect
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderPhoto
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.repository.PhotoRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class PurgeExpiredRemindersUseCaseTest {

    private val now = Instant.parse("2026-11-07T12:00:00Z")
    private val repository = FakeReminderRepository()
    private val files = FakeFiles()
    private val purge = PurgeExpiredRemindersUseCase(
        repository,
        DeleteRemindersForeverUseCase(repository, FakeReminderScheduler(), files),
        now = { now },
    )

    private suspend fun saved(status: ReminderStatus, isDone: Boolean, daysAgo: Long, photo: String? = null): Long =
        repository.save(
            Reminder(
                title = "x",
                status = status,
                isDone = isDone,
                statusChangedAt = now.minus(Duration.ofDays(daysAgo)),
                photos = listOfNotNull(photo?.let { ReminderPhoto(path = it) }),
                createdAt = Instant.EPOCH,
            ),
        )

    @Test
    fun `borra los hechos de hace 7 dias o mas y conserva los mas nuevos`() = runBlocking {
        val old = saved(ReminderStatus.Active, isDone = true, daysAgo = 7)
        val recent = saved(ReminderStatus.Active, isDone = true, daysAgo = 6)

        assertEquals(1, purge())
        assertNull(repository.getById(old))
        assertNotNull(repository.getById(recent))
    }

    @Test
    fun `borra los eliminados de hace 15 dias o mas, con sus fotos`() = runBlocking {
        val old = saved(ReminderStatus.Deleted, isDone = false, daysAgo = 15, photo = "/fotos/a.jpg")
        val recent = saved(ReminderStatus.Deleted, isDone = false, daysAgo = 14)

        assertEquals(1, purge())
        assertNull(repository.getById(old))
        assertNotNull(repository.getById(recent))
        assertEquals(listOf("/fotos/a.jpg"), files.deleted)
    }

    @Test
    fun `nunca borra archivados ni activos`() = runBlocking {
        val archived = saved(ReminderStatus.Archived, isDone = false, daysAgo = 400)
        val active = repository.save(Reminder(title = "y", createdAt = Instant.EPOCH))

        assertEquals(0, purge())
        assertNotNull(repository.getById(archived))
        assertNotNull(repository.getById(active))
    }

    /** Anota qué archivos se pidió borrar. */
    private class FakeFiles : PhotoRepository {
        val deleted = mutableListOf<String>()
        override suspend fun import(uri: String): String = uri
        override suspend fun saveCover(sourcePath: String, crop: CropRect): String = sourcePath
        override suspend fun delete(path: String) {
            deleted += path
        }
    }
}
