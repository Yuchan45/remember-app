package com.example.uade.rememberapp.ui.reminders.detail

import androidx.annotation.DrawableRes
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.Importance
import com.example.uade.rememberapp.domain.model.MAX_PHOTOS
import com.example.uade.rememberapp.domain.model.MessageChannel
import com.example.uade.rememberapp.domain.model.NotificationAction
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderPhoto
import com.example.uade.rememberapp.domain.model.ReminderType
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
    /** Nombre de cada lugar por id, para mostrar "Al llegar a Casa". */
    val placeNames: Map<Long, String> = emptyMap(),
    val placesDraft: List<PlaceAlert>? = null,
    val isCreatingPlace: Boolean = false,
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
    val favoritePlaces: List<Place> = emptyList(),
    /** El modal "Ubicación" mientras está abierto (null = cerrado). */
    val placeEditor: PlaceEditor? = null,

    /** Las fotos elegidas (hasta MAX_PHOTOS), en el orden en que se eligieron; una puede ser la portada. */
    val photos: List<ReminderPhoto> = emptyList(),
    /** La portada ya recortada (se ve arriba de la descripción y en la card); null = sin portada. */
    val photoPath: String? = null,
    /** El modal "Imágenes" mientras está abierto (null = cerrado). Copia de trabajo: "Guardar" pasa
     * sus fotos y su portada a [photos] y [photoPath]; "Cancelar" la descarta.
     */
    val photosEditor: PhotosEditor? = null,

    /** Tipo de recordatorio (Nota o Checklist). */
    val type: ReminderType = ReminderType.Note,
    /** Ítems si es de tipo lista (Checklist). */
    val items: List<ChecklistItem> = emptyList(),
    /** Si la sección COMPLETADOS está desplegada. */
    val isCompletedExpanded: Boolean = true,
    /** Índice del ítem cuya nota está abierta para edición (null = ninguna abierta). */
    val expandedItemNoteIndex: Int? = null,
    /** Modal para pegar texto y parsear con IA. */
    val isPasteDialogOpen: Boolean = false,
    /** Sheet de captura por voz para dictar ítems. */
    val isVoiceCaptureOpen: Boolean = false,
    /** Indicador de progreso mientras la IA genera/parsea ítems. */
    val isGeneratingItems: Boolean = false,

    /** true mientras se está guardando con ✓ para evitar guardados duplicados. */
    val isSaving: Boolean = false,
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
     * - Nota nueva: hace falta título, descripción o portada (solo etiquetas o avisos no alcanza).
     * - Existente: cambió el título, la descripción, las etiquetas, los avisos por hora, la
     *   importancia, la acción de notificación o las fotos.
     */
    val hasUnsavedChanges: Boolean
        get() = when {
            isNew -> cleanTitle != null || cleanDescription != null || photoPath != null || items.isNotEmpty() || alarms.isNotEmpty() || places.isNotEmpty()
            reminder != null -> cleanTitle != reminder.title ||
                cleanDescription != reminder.description ||
                tagIds != reminder.tags.map { it.id }.toSet() ||
                alarms != reminder.alarms.map { it.at } ||
                places != reminder.places ||
                importance != reminder.importance ||
                action != reminder.action ||
                photos != reminder.photos ||
                photoPath != reminder.photoPath ||
                items != reminder.items ||
                type != reminder.type
            else -> false
        }

    /**
     * Las etiquetas asignadas, con su nombre y color actual. Si una se borró, ya no está en
     * [allTags] y queda afuera sola.
     */
    val tags: List<Tag> get() = allTags.filter { it.id in tagIds }

    // Lo que muestran el encabezado y el panel de opciones. Una nota nueva todavía no tiene
    // nada asignado: se ve igual que una nota sin datos.

    /** Avisos por lugar (el borrador si se editó o el del recordatorio). */
    val places: List<PlaceAlert> get() = placesDraft ?: reminder?.places.orEmpty()
    val placeName: String? get() = places.firstOrNull()?.let { placeNames[it.placeId] }

    /**
     * Las fotos en el orden en que se apilan arriba de la descripción: primero la portada ya
     * recortada y después las demás (sin repetir la original de la portada). Sin portada, todas
     * en el orden en que se eligieron.
     */
    val stackPhotos: List<String>
        get() = if (photoPath != null) {
            listOf(photoPath) + photos.filterNot { it.isCover }.map { it.path }
        } else {
            photos.map { it.path }
        }

    @get:DrawableRes
    val typeIcon: Int
        get() = if (type == ReminderType.Checklist) R.drawable.ic_checklist else (reminder?.typeIcon() ?: R.drawable.ic_description)

    /** El título y la descripción como se guardarían: sin espacios de los bordes, vacío = null. */
    val cleanTitle: String? get() = title.trim().ifEmpty { null }
    val cleanDescription: String? get() = description.trim().ifEmpty { null }

    /**
     * Los avisos como se guardarían, para armar el Reminder. Los que ya existían conservan si eran
     * un aviso pospuesto (la pantalla solo maneja las horas).
     */
    val alarmsToSave: List<Alarm>
        get() = alarms.map { at -> reminder?.alarms?.firstOrNull { it.at == at } ?: Alarm(at) }
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

/**
 * El modal "Imágenes": las fotos como quedan en el modal y la portada recortada.
 *
 * - [isImporting]: se están copiando fotos recién elegidas (se muestra "Cargando fotos…").
 * - [cropping]: la foto que se está recortando (abre el recorte encima del modal); null = ninguna.
 * - [isSavingCover]: se está guardando el recorte (evita guardarlo dos veces con doble toque).
 */
data class PhotosEditor(
    val photos: List<ReminderPhoto> = emptyList(),
    val coverPath: String? = null,
    val isImporting: Boolean = false,
    val cropping: String? = null,
    val isSavingCover: Boolean = false,
) {
    /** Cuántas fotos más se pueden agregar. */
    val freeSlots: Int get() = MAX_PHOTOS - photos.size
}
