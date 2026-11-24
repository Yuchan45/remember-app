package com.example.uade.rememberapp.data.local.mapper

import com.example.uade.rememberapp.data.local.entity.ActionEntity
import com.example.uade.rememberapp.data.local.entity.AlarmEntity
import com.example.uade.rememberapp.data.local.entity.PlaceAlertEntity
import com.example.uade.rememberapp.data.local.entity.ReminderWithDetails
import com.example.uade.rememberapp.data.local.entity.TagEntity
import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.Importance
import com.example.uade.rememberapp.domain.model.MessageChannel
import com.example.uade.rememberapp.domain.model.NotificationAction
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.domain.model.PlaceAlertEvent
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderPhoto
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ReminderMapperTest {

    private val createdAt = Instant.parse("2026-09-30T12:00:00Z")

    private val reminder = Reminder(
        id = 7,
        title = "Llamar al plomero",
        status = ReminderStatus.Archived,
        createdAt = createdAt,
    )

    @Test
    fun `ida y vuelta de la fila conserva los datos propios del recordatorio`() {
        assertEquals(reminder, reminder.toEntity().toDomain())
    }

    @Test
    fun `trae del resultado de Room las etiquetas y los avisos, con las horas ordenadas`() {
        val row = ReminderWithDetails(
            reminder = reminder.toEntity(),
            tags = listOf(TagEntity(id = 1, name = "Salud", colorArgb = 0xFF6FCF97)),
            alarms = listOf(
                AlarmEntity(id = 2, reminderId = 7, atMillis = 2_000),
                AlarmEntity(id = 1, reminderId = 7, atMillis = 1_000),
            ),
            places = listOf(PlaceAlertEntity(id = 1, reminderId = 7, placeId = 3, event = "Leave")),
        )

        val domain = row.toDomain()

        assertEquals(listOf(Tag(id = 1, name = "Salud", colorArgb = 0xFF6FCF97)), domain.tags)
        assertEquals(listOf(Alarm(Instant.ofEpochMilli(1_000)), Alarm(Instant.ofEpochMilli(2_000))), domain.alarms)
        assertEquals(listOf(PlaceAlert(placeId = 3, event = PlaceAlertEvent.Leave)), domain.places)
    }

    @Test
    fun `un aviso por lugar va y vuelve igual`() {
        val alert = PlaceAlert(placeId = 3, event = PlaceAlertEvent.Leave)
        assertEquals(alert, alert.toEntity(reminderId = 7).toDomain())
    }

    @Test
    fun `cada acción de notificación va a su fila y vuelve igual`() {
        listOf(
            NotificationAction.Call(phone = "11 5555-5555"),
            NotificationAction.Message(MessageChannel.WhatsApp, to = "11 5555-5555", text = "Llego tarde"),
            NotificationAction.Message(MessageChannel.Instagram, to = "ana"),
            NotificationAction.Email(to = "ana@mail.com", subject = "Turno"),
        ).forEach { action ->
            assertEquals(action, action.toEntity(reminderId = 7).toDomain())
        }
    }

    @Test
    fun `ida y vuelta conserva el audioPath y aiStatus`() {
        val original = reminder.copy(
            audioPath = "/data/user/0/app/files/audios/rec_1.m4a",
            aiStatus = com.example.uade.rememberapp.domain.model.AiProcessingStatus.Pending,
        )
        assertEquals(original, original.toEntity().toDomain())
    }

    @Test
    fun `la acción llega al recordatorio desde el resultado de Room`() {
        val row = ReminderWithDetails(
            reminder = reminder.toEntity(),
            tags = emptyList(),
            alarms = emptyList(),
            places = emptyList(),
            actions = listOf(NotificationAction.Call("11 5555-5555").toEntity(reminderId = 7)),
        )

        assertEquals(NotificationAction.Call("11 5555-5555"), row.toDomain().action)
    }

    @Test
    fun `un aviso pospuesto conserva la marca al guardarse y leerse`() {
        val snooze = Alarm(Instant.ofEpochMilli(5_000), isSnooze = true)

        assertEquals(snooze, snooze.toEntity(reminderId = 7).toDomain())
    }

    @Test
    fun `las fotos vuelven en el orden en que se eligieron y con su portada`() {
        val photos = listOf(
            ReminderPhoto("/fotos/a.jpg"),
            ReminderPhoto("/fotos/b.jpg", isCover = true),
            ReminderPhoto("/fotos/c.jpg"),
        )
        // Room puede devolver las filas en cualquier orden: se mezclan a propósito.
        val rows = photos.mapIndexed { i, photo -> photo.toEntity(position = i, reminderId = 7) }.reversed()
        val row = ReminderWithDetails(
            reminder = reminder.copy(photoPath = "/fotos/portada.jpg").toEntity(),
            tags = emptyList(),
            alarms = emptyList(),
            places = emptyList(),
            photos = rows,
        )

        val domain = row.toDomain()
        assertEquals(photos, domain.photos)
        assertEquals("/fotos/portada.jpg", domain.photoPath)
    }

    @Test
    fun `una acción de tipo desconocido se ignora y un canal desconocido pasa a SMS`() {
        val unknownType = ActionEntity(reminderId = 7, type = "Fax", channel = null, target = "123", text = null)
        val unknownChannel = ActionEntity(reminderId = 7, type = "Message", channel = "Telegram", target = "123", text = null)

        assertEquals(null, unknownType.toDomain())
        assertEquals(MessageChannel.Sms, (unknownChannel.toDomain() as NotificationAction.Message).channel)
    }

    @Test
    fun `la importancia se guarda por nombre y vuelve igual`() {
        val critical = reminder.copy(importance = Importance.Critical)

        assertEquals("Critical", critical.toEntity().importance)
        assertEquals(critical, critical.toEntity().toDomain())
    }

    @Test
    fun `un evento de lugar, un estado o una importancia desconocidos no rompen la lectura`() {
        val place = PlaceAlertEntity(reminderId = 7, placeId = 3, event = "OTRO").toDomain()
        val domain = reminder.toEntity().copy(status = "Viejo", importance = "Urgente").toDomain()

        assertEquals(PlaceAlertEvent.Arrive, place.event)
        assertEquals(ReminderStatus.Active, domain.status)
        assertEquals(Importance.Default, domain.importance)
    }

    @Test
    fun `los items de checklist van a su fila y vuelven en orden con su nota y tilde`() {
        val items = listOf(
            com.example.uade.rememberapp.domain.model.ChecklistItem(id = 1, text = "Vacío 5kg", note = "En el Coto"),
            com.example.uade.rememberapp.domain.model.ChecklistItem(id = 2, text = "Asado 3kg", note = "En Res", isChecked = true),
        )
        val rows = items.mapIndexed { i, item -> item.toEntity(position = i, reminderId = 7) }.reversed()
        val row = ReminderWithDetails(
            reminder = reminder.copy(type = com.example.uade.rememberapp.domain.model.ReminderType.Checklist).toEntity(),
            tags = emptyList(),
            alarms = emptyList(),
            places = emptyList(),
            items = rows,
        )

        val domain = row.toDomain()
        assertEquals(items.map { it.text }, domain.items.map { it.text })
        assertEquals(items.map { it.note }, domain.items.map { it.note })
        assertEquals(items.map { it.isChecked }, domain.items.map { it.isChecked })
    }
}
