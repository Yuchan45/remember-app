package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.AiAnalysisResult
import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.ProposedReminder
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.model.ReminderType
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
            alarms = listOf(Alarm(Instant.parse("2026-10-09T09:00:00Z"))),
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
        assertEquals(p1.alarms, merged.alarms)
        assertEquals("Casa", merged.placeName)
    }

    @Test
    fun `SplitProposedReminderUseCase desune una propuesta unida restaurando las originales`() {
        val mergeUseCase = MergeProposedRemindersUseCase()
        val splitUseCase = SplitProposedReminderUseCase()

        val p1 = ProposedReminder(
            title = "Hacer la tarea",
            triggerDisplayTime = "Hoy 21:00",
            listName = "Estudio",
            isSelected = true,
        )
        val p2 = ProposedReminder(
            title = "Ir al súper",
            placeName = "Al pasar por Súper",
            listName = "Súper",
            isSelected = true,
        )

        val merged = mergeUseCase(listOf(p1, p2))
        assertEquals(ReminderType.Checklist, merged.type)

        val unmerged = splitUseCase(merged)
        assertEquals(2, unmerged.size)
        assertEquals("Hacer la tarea", unmerged[0].title)
        assertEquals("Hoy 21:00", unmerged[0].triggerDisplayTime)
        assertEquals("Ir al súper", unmerged[1].title)
        assertEquals("Al pasar por Súper", unmerged[1].placeName)
    }

    @Test
    fun `SplitProposedReminderUseCase separa una checklist en recordatorios independientes`() {
        val splitUseCase = SplitProposedReminderUseCase()

        val checklist = ProposedReminder(
            title = "Viaje a Bariloche",
            type = ReminderType.Checklist,
            items = listOf(
                ChecklistItem(text = "Sacar la campera"),
                ChecklistItem(text = "Comprar protector solar"),
            ),
            listName = "Viaje",
            placeName = "Casa",
        )

        val unmerged = splitUseCase(checklist)
        assertEquals(2, unmerged.size)
        assertEquals("Sacar la campera", unmerged[0].title)
        assertEquals(ReminderType.Note, unmerged[0].type)
        assertEquals("Viaje", unmerged[0].listName)
        assertEquals("Comprar protector solar", unmerged[1].title)
        assertEquals(ReminderType.Note, unmerged[1].type)
    }

    @Test
    fun `DetectSimilarRemindersUseCase detecta recordatorio cercano en horario`() {
        val useCase = DetectSimilarRemindersUseCase()
        val baseTime = Instant.parse("2026-10-08T22:00:00Z")

        val existing = Reminder(
            id = 10,
            title = "Cepillar los dientes",
            alarms = listOf(Alarm(baseTime)),
            status = ReminderStatus.Active,
            createdAt = baseTime.minusSeconds(3600),
        )

        val newProposal = ProposedReminder(
            title = "Cambiar las sábanas",
            alarms = listOf(Alarm(baseTime.plusSeconds(1800))), // 30 min despues
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
            alarms = listOf(Alarm(baseTime)),
            status = ReminderStatus.Active,
            createdAt = baseTime.minusSeconds(3600),
        )

        val newProposal = ProposedReminder(
            title = "Cambiar las sábanas",
            alarms = listOf(Alarm(baseTime.plusSeconds(36000))), // 10 horas despues
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
