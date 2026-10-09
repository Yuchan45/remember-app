package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.AiAnalysisResult
import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.ProposedReminder
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.model.Trigger
import com.example.uade.rememberapp.domain.repository.AiReminderService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AiRemindersUseCasesTest {

    @Test
    fun `MergeProposedRemindersUseCase combina multiples propuestas en una sola checklist`() {
        val useCase = MergeProposedRemindersUseCase()

        val p1 = ProposedReminder(
            title = "Recibir al técnico del aire",
            triggerDisplayTime = "Mañana 09:00",
            trigger = Trigger.AtTime(Instant.parse("2026-10-09T09:00:00Z")),
            placeName = "Casa",
            priorityName = "Alta",
            isSelected = true,
        )
        val p2 = ProposedReminder(
            title = "Comprar papas",
            listName = "Súper",
            placeName = "Al pasar por Súper",
            isSelected = true,
        )

        val merged = useCase(listOf(p1, p2))

        assertEquals(ReminderType.Checklist, merged.type)
        assertEquals(2, merged.items.size)
        assertEquals("Recibir al técnico del aire", merged.items[0].text)
        assertEquals("Comprar papas", merged.items[1].text)
        assertEquals(p1.trigger, merged.trigger)
        assertEquals("Casa", merged.placeName)
    }

    @Test
    fun `DetectSimilarRemindersUseCase detecta recordatorio cercano en horario`() {
        val useCase = DetectSimilarRemindersUseCase()
        val baseTime = Instant.parse("2026-10-08T22:00:00Z")

        val existing = Reminder(
            id = 10,
            title = "Cepillar los dientes",
            trigger = Trigger.AtTime(baseTime),
            status = ReminderStatus.Active,
            createdAt = baseTime.minusSeconds(3600),
        )

        val newProposal = ProposedReminder(
            title = "Cambiar las sábanas",
            trigger = Trigger.AtTime(baseTime.plusSeconds(1800)), // 30 min despues
            triggerDisplayTime = "Esta noche",
        )

        val proposal = useCase(newProposal, listOf(existing))

        assertNotNull(proposal)
        assertEquals(2, proposal!!.checklistItems.size)
        assertEquals("Cepillar los dientes", proposal.checklistItems[0].text)
        assertEquals("Cambiar las sábanas", proposal.checklistItems[1].text)
    }

    @Test
    fun `DetectSimilarRemindersUseCase no sugiere merge si los horarios difieren mucho`() {
        val useCase = DetectSimilarRemindersUseCase()
        val baseTime = Instant.parse("2026-10-08T10:00:00Z")

        val existing = Reminder(
            id = 10,
            title = "Cepillar los dientes",
            trigger = Trigger.AtTime(baseTime),
            status = ReminderStatus.Active,
            createdAt = baseTime.minusSeconds(3600),
        )

        val newProposal = ProposedReminder(
            title = "Cambiar las sábanas",
            trigger = Trigger.AtTime(baseTime.plusSeconds(36000)), // 10 horas despues
        )

        val proposal = useCase(newProposal, listOf(existing))
        assertNull(proposal)
    }

    @Test
    fun `ProcessVoiceWithAiUseCase invoca servicio de IA con timestamp ISO`() = runBlocking {
        var recordedIso: String? = null
        val fakeService = object : AiReminderService {
            override suspend fun transcribeAudio(audioFilePath: String): Result<String> = Result.success("ok")
            override suspend fun analyzeText(text: String, referenceDateTimeIso: String): Result<AiAnalysisResult> =
                Result.success(AiAnalysisResult(rawTranscript = text, summaryBadge = "", proposedReminders = emptyList()))

            override suspend fun processAudio(audioFilePath: String, referenceDateTimeIso: String): Result<AiAnalysisResult> {
                recordedIso = referenceDateTimeIso
                return Result.success(
                    AiAnalysisResult(
                        rawTranscript = "Test transcript",
                        summaryBadge = "Badge",
                        proposedReminders = listOf(ProposedReminder(title = "Test")),
                    ),
                )
            }
        }

        val useCase = ProcessVoiceWithAiUseCase(fakeService)
        val result = useCase("/path/audio.m4a", referenceTime = Instant.parse("2026-10-08T20:00:00Z"))

        assertTrue(result.isSuccess)
        assertNotNull(recordedIso)
        assertEquals("Test transcript", result.getOrNull()?.rawTranscript)
    }
}
