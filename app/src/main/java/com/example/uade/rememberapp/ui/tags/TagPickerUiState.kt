package com.example.uade.rememberapp.ui.tags

import androidx.compose.ui.graphics.toArgb
import com.example.uade.rememberapp.domain.model.Tag
import com.example.uade.rememberapp.domain.repository.InvalidTagNameException
import com.example.uade.rememberapp.ui.components.tagColorForHue

/** Tono con el que arranca el editor para una etiqueta nueva (celeste). */
const val DefaultTagHue = 180f

/** Color ARGB (el formato de [Tag.colorArgb]) para un tono. */
fun tagColorArgbForHue(hue: Float): Long = tagColorForHue(hue).toArgb().toLong() and 0xFFFFFFFFL

/**
 * Estado del diálogo de etiquetas.
 *
 * - [tags]: todas las etiquetas (vienen del repositorio).
 * - [assignedIds]: las asignadas al recordatorio que se está creando; el resto son Disponibles.
 * - Editor: [editorName] y [editorColorArgb]. Si [editingId] no es null se está editando esa
 *   etiqueta; si es null, el editor crea una nueva.
 * - [recentlyDeleted]: la última etiqueta borrada, mientras se puede deshacer.
 * - [isCreatorExpanded]: si está desplegado el editor (crear o editar una etiqueta).
 */
data class TagPickerUiState(
    val tags: List<Tag> = emptyList(),
    val assignedIds: Set<Long> = emptySet(),
    val editorName: String = "",
    val editorHue: Float = DefaultTagHue,
    val editorColorArgb: Long = tagColorArgbForHue(DefaultTagHue),
    val editingId: Long? = null,
    val error: InvalidTagNameException.Reason? = null,
    val recentlyDeleted: DeletedTag? = null,
    /** Si se ve la parte para crear y editar (nombre, color). Arranca plegada. */
    val isCreatorExpanded: Boolean = false,
) {
    val assigned: List<Tag> get() = tags.filter { it.id in assignedIds }
    val available: List<Tag> get() = tags.filterNot { it.id in assignedIds }
    val editingTag: Tag? get() = tags.firstOrNull { it.id == editingId }
    val isEditing: Boolean get() = editingId != null
}

/** Etiqueta recién borrada y si estaba asignada, para devolverla igual al deshacer. */
data class DeletedTag(
    val tag: Tag,
    val wasAssigned: Boolean,
)
