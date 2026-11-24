package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ChecklistCompletionTest {

    private val repository = FakeReminderRepository()
    private val scheduler = FakeReminderScheduler()
    private val markReminderDone = MarkReminderDoneUseCase(repository, scheduler)

    @Test
    fun `no se puede finalizar una checklist si tiene items sin marcar`() = runBlocking {
        val reminder = Reminder(
            type = ReminderType.Checklist,
            title = "Lista de compras",
            items = listOf(
                ChecklistItem(id = 1, text = "Pan", isChecked = false),
                ChecklistItem(id = 2, text = "Leche", isChecked = true),
            ),
            createdAt = Instant.EPOCH,
        )
        val id = repository.save(reminder)

        val saved = repository.getById(id)!!
        val canFinalize = saved.items.isNotEmpty() && saved.items.all { it.isChecked }

        assertFalse("No debería poder finalizarse porque 'Pan' no está completado", canFinalize)
    }

    @Test
    fun `se puede finalizar una checklist si todos los items estan marcados`() = runBlocking {
        val reminder = Reminder(
            type = ReminderType.Checklist,
            title = "Lista de compras",
            items = listOf(
                ChecklistItem(id = 1, text = "Pan", isChecked = true),
                ChecklistItem(id = 2, text = "Leche", isChecked = true),
            ),
            createdAt = Instant.EPOCH,
        )
        val id = repository.save(reminder)

        val saved = repository.getById(id)!!
        val canFinalize = saved.items.isNotEmpty() && saved.items.all { it.isChecked }

        assertTrue("Debería poder finalizarse porque todos los ítems están completados", canFinalize)

        markReminderDone(id)
        val finalized = repository.getById(id)!!
        assertTrue(finalized.isDone)
    }
}
