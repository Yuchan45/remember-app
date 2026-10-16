package com.example.uade.rememberapp.ui.reminders.detail

import androidx.annotation.DrawableRes
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.Importance
import com.example.uade.rememberapp.domain.model.MessageChannel
import com.example.uade.rememberapp.domain.model.NotificationAction
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.Tag
import com.example.uade.rememberapp.ui.reminders.model.RepeatOption
import com.example.uade.rememberapp.ui.reminders.model.TimeShortcut
import com.example.uade.rememberapp.ui.reminders.components.typeIcon
import com.example.uade.rememberapp.ui.reminders.model.NotificationActionKind
import com.example.uade.rememberapp.ui.reminders.model.PlaceEvent
import com.example.uade.rememberapp.ui.reminders.sample.SampleQuickCapture
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Estado de la pantalla de un recordatorio. Sirve para dos casos:
 *
 * - **Existente** (se abrió tocando una card):
 *   - cargando: [isLoading] en true (todavía no se leyó de la base);
 *   - encontrado: [reminder] con datos;
 *   - no encontrado: [isLoading] en false y [reminder] null (ej. se borró mientras tanto).
 * - **Nueva** ([isNew], se abrió con "+" → "Nota"): no hay [reminder] todavía; se crea al
 *   tocar ✓.
 *
 * [title], [description], [tagIds] y [alarms] son el borrador: se guardan recién con ✓.
 */
data class ReminderDetailUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = false,
    val reminder: Reminder? = null,
    /**
     * Nombre del primer lugar de aviso (PlaceAlert solo guarda el id).
     * TODO: tomarlo de PlaceRepository cuando existan los lugares guardados.
     */
    val placeName: String? = null,
    val title: String = "",
    val description: String = "",
    /** Las etiquetas asignadas en el modal de etiquetas. */
    val tagIds: Set<Long> = emptySet(),
    /** Todas las etiquetas del usuario, al día (para mostrar las asignadas con nombre y color). */
    val allTags: List<Tag> = emptyList(),
    /**
     * El modal de etiquetas mientras está abierto (null = cerrado): las asignadas como quedan en
     * el modal. Es una copia de trabajo: "Guardar" la pasa a [tagIds] y "Cancelar" la descarta.
     */
    val tagsEditor: Set<Long>? = null,

    /** Los avisos por hora, ordenados (lo que muestra la fila "Recordatorio"). */
    val alarms: List<Instant> = emptyList(),
    /**
     * El modal "Establecer recordatorio" mientras está abierto (null = cerrado). Es una copia
     * de trabajo: "Guardar" la pasa a [alarms] y "Cancelar" la descarta.
     */
    val alarmsEditor: AlarmsEditor? = null,

    /** Qué tan insistente es el aviso (fila "Comportamiento"). */
    val importance: Importance = Importance.Default,
    /**
     * El modal "Cómo avisar" mientras está abierto (null = cerrado): el nivel marcado en el modal.
     * "Guardar" lo pasa a [importance] y "Cancelar" lo descarta.
     */
    val importanceEditor: Importance? = null,

    /** La acción rápida de la notificación (fila "Acción de notificación"); null = sin acción. */
    val action: NotificationAction? = null,
    /** El modal "Acción de notificación" mientras está abierto (null = cerrado). */
    val actionEditor: ActionEditor? = null,

    /**
     * Los lugares favoritos que se ofrecen en el modal "Ubicación".
     * TODO: tomarlos de PlaceRepository cuando existan los lugares guardados.
     */
    val favoritePlaces: List<Place> = SampleQuickCapture.favoritePlaces,
    /** El modal "Ubicación" mientras está abierto (null = cerrado). */
    val placeEditor: PlaceEditor? = null,

    /** true cuando se guardó con ✓: la pantalla lo ve y se cierra. */
    val isSaved: Boolean = false,
) {
    val isNotFound: Boolean get() = !isLoading && !isNew && reminder == null

    /** Si hay algo para mostrar y editar (una nota existente cargada, o una nueva). */
    val isEditable: Boolean get() = isNew || reminder != null

    /**
     * true si hay algo para guardar (el ✓ se pinta de verde). Compara como se guardaría: sin
     * espacios de los bordes y con "vacío" igual a null.
     *
     * - Nota nueva: hace falta título o descripción (solo etiquetas o avisos no alcanza).
     * - Existente: cambió el título, la descripción, las etiquetas, los avisos por hora, la
     *   importancia o la acción de notificación.
     */
    val hasUnsavedChanges: Boolean
        get() = when {
            isNew -> cleanTitle != null || cleanDescription != null
            reminder != null -> cleanTitle != reminder.title ||
                cleanDescription != reminder.description ||
                tagIds != reminder.tags.map { it.id }.toSet() ||
                alarms != reminder.alarms.map { it.at } ||
                importance != reminder.importance ||
                action != reminder.action
            else -> false
        }

    /**
     * Las etiquetas asignadas, con su nombre y color actual. Si una se borró, ya no está en
     * [allTags] y queda afuera sola.
     */
    val tags: List<Tag> get() = allTags.filter { it.id in tagIds }

    // Lo que muestran el encabezado y el panel de opciones. Una nota nueva todavía no tiene
    // nada asignado: se ve igual que una nota sin datos.

    /** Los avisos por lugar se conservan como vienen (todavía no hay modal para editarlos). */
    val places: List<PlaceAlert> get() = reminder?.places.orEmpty()
    val photoPath: String? get() = reminder?.photoPath

    @get:DrawableRes
    val typeIcon: Int get() = reminder?.typeIcon() ?: R.drawable.ic_description

    /** El título y la descripción como se guardarían: sin espacios de los bordes, vacío = null. */
    val cleanTitle: String? get() = title.trim().ifEmpty { null }
    val cleanDescription: String? get() = description.trim().ifEmpty { null }

    /** Los avisos como se guardarían, para armar el Reminder. */
    val alarmsToSave: List<Alarm> get() = alarms.map { Alarm(it) }
}

/**
 * El modal "Establecer recordatorio": una tarjeta por aviso, cada una con los mismos chips que la
 * nota rápida.
 *
 * - [expanded]: qué tarjeta está desplegada (una a la vez; null = todas plegadas).
 * - [datePickerFor]: para qué tarjeta se abrió el calendario de "Elegir fecha…".
 */
data class AlarmsEditor(
    val items: List<AlarmDraft>,
    val expanded: Int? = 0,
    val datePickerFor: Int? = null,
)

/**
 * Un aviso dentro del modal, como lo está eligiendo el usuario.
 *
 * [at] es el momento ya calculado (null = todavía no eligió nada en esta tarjeta). El resto es
 * para dibujar los chips: qué atajo está marcado y, con "Elegir fecha…", qué día y hora.
 */
data class AlarmDraft(
    val at: Instant? = null,
    val selectedTime: TimeShortcut? = null,
    val pickedDate: LocalDate? = null,
    val pickedTime: LocalTime? = null,
    /** TODO: la repetición todavía no se guarda en el recordatorio; por ahora solo se marca. */
    val selectedRepeat: RepeatOption = RepeatOption.None,
)

/**
 * El modal "Acción de notificación", que tiene dos pasos:
 *
 * - [ActionEditorStep.List]: la lista de acciones ("Llamar al número", "Enviar mensaje"…).
 * - [ActionEditorStep.Form]: los datos de la acción elegida ([kind]): el canal (solo mensajes),
 *   el destinatario ([target]) y el texto o asunto opcional ([text]).
 *
 * [hadAction] es si el recordatorio ya tenía una acción al abrir el modal (para "Quitar acción").
 */
data class ActionEditor(
    val step: ActionEditorStep = ActionEditorStep.List,
    val kind: NotificationActionKind? = null,
    val channel: MessageChannel = MessageChannel.WhatsApp,
    val target: String = "",
    val text: String = "",
    val hadAction: Boolean = false,
) {
    /** Hace falta un destinatario para guardar. */
    val canSave: Boolean get() = kind != null && target.isNotBlank()
}

enum class ActionEditorStep {
    List,
    Form,
}

/**
 * El modal "Ubicación": el lugar marcado ([selectedPlaceId], null = ninguno) y si avisa al llegar
 * o al salir ([event]).
 *
 * TODO: por ahora solo se marca; "Guardar" no cambia los avisos por lugar del recordatorio.
 */
data class PlaceEditor(
    val selectedPlaceId: Long? = null,
    val event: PlaceEvent = PlaceEvent.Arrive,
)
