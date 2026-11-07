package com.example.uade.rememberapp.ui.reminders.review

import com.example.uade.rememberapp.domain.model.AiAnalysisResult
import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.ProposedReminder
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.usecase.FakeReminderRepository
import com.example.uade.rememberapp.domain.usecase.FakeReminderScheduler
import com.example.uade.rememberapp.domain.usecase.MergeProposedRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.SaveReminderUseCase
import com.example.uade.rememberapp.domain.usecase.SplitProposedReminderUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** El caso del bug: 2 listas → "Separados" → "Checklist" dejaba 1 sola. */
class ReviewProposedRemindersViewModelTest {

    private val viewModel = ReviewProposedRemindersViewModel(
        saveReminder = SaveReminderUseCase(FakeReminderRepository(), FakeReminderScheduler()),
        mergeProposedReminders = MergeProposedRemindersUseCase(),
        splitProposedReminder = SplitProposedReminderUseCase(),
    )

    private val result = AiAnalysisResult(
        rawTranscript = "mañana ir a lo de Guido con facturas y medialunas, y armar el bolso con toallas",
        summaryBadge = "Encontré 2 recordatorios.",
        isChecklistCandidate = true,
        proposedReminders = listOf(
            ProposedReminder(
                tempId = "guido",
                title = "Ir a la casa de Guido",
                type = ReminderType.Checklist,
                items = listOf(ChecklistItem(text = "Facturas"), ChecklistItem(text = "Medialunas")),
            ),
            ProposedReminder(
                tempId = "bolso",
                title = "Armar el bolso",
                type = ReminderType.Checklist,
                items = listOf(ChecklistItem(text = "Toallas")),
            ),
        ),
    )

    @Test
    fun `separados y volver a checklist deja las mismas dos listas`() {
        viewModel.initialize(result)

        viewModel.onSetChecklistView(false)
        assertEquals(3, viewModel.uiState.value.proposals.size) // un recordatorio por ítem
        assertFalse(viewModel.uiState.value.isChecklistView)

        viewModel.onSetChecklistView(true)
        assertEquals(listOf("guido", "bolso"), viewModel.uiState.value.proposals.map { it.tempId })
    }

    @Test
    fun `el contador cuenta recordatorios y no items`() {
        viewModel.initialize(result)

        assertEquals(2, viewModel.uiState.value.selectedCount)
    }

    @Test
    fun `volver a mostrar la pantalla no reinicia lo que cambio el usuario`() {
        viewModel.initialize(result)
        viewModel.onToggleProposalSelection("bolso")

        viewModel.initialize(result) // ej. al volver a la app después de minimizarla

        assertEquals(1, viewModel.uiState.value.selectedCount)
    }
}
