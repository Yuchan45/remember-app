package com.example.uade.rememberapp.ui.reminders.detail

import androidx.annotation.DrawableRes
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.Tag
import com.example.uade.rememberapp.domain.model.Trigger
import com.example.uade.rememberapp.ui.reminders.components.typeIcon

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
 * [title] y [description] son lo que el usuario está escribiendo; se guardan recién con ✓.
 */
data class ReminderDetailUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = false,
    val reminder: Reminder? = null,
    /**
     * Nombre del lugar del aviso, si es por lugar (Trigger.AtPlace solo guarda el id).
     * TODO: tomarlo de PlaceRepository cuando existan los lugares guardados.
     */
    val placeName: String? = null,
    val title: String = "",
    val description: String = "",
    /** true cuando se guardó con ✓: la pantalla lo ve y se cierra. */
    val isSaved: Boolean = false,
) {
    val isNotFound: Boolean get() = !isLoading && !isNew && reminder == null

    /** Si hay algo para mostrar y editar (una nota existente cargada, o una nueva). */
    val isEditable: Boolean get() = isNew || reminder != null

    /**
     * true si lo escrito difiere de lo guardado (el ✓ se pinta de verde). Compara como se
     * guardaría: sin espacios de los bordes y con "vacío" igual a null. En una nota nueva,
     * cualquier texto cuenta como cambio.
     */
    val hasUnsavedChanges: Boolean
        get() = when {
            isNew -> cleanTitle != null || cleanDescription != null
            reminder != null -> cleanTitle != reminder.title || cleanDescription != reminder.description
            else -> false
        }

    // Lo que muestran el encabezado y el panel de opciones. Una nota nueva todavía no tiene
    // nada asignado: se ve igual que una nota sin datos.
    val tags: List<Tag> get() = reminder?.tags.orEmpty()
    val trigger: Trigger get() = reminder?.trigger ?: Trigger.None
    val photoPath: String? get() = reminder?.photoPath

    @get:DrawableRes
    val typeIcon: Int get() = reminder?.typeIcon() ?: R.drawable.ic_description

    /** El título y la descripción como se guardarían: sin espacios de los bordes, vacío = null. */
    val cleanTitle: String? get() = title.trim().ifEmpty { null }
    val cleanDescription: String? get() = description.trim().ifEmpty { null }
}
