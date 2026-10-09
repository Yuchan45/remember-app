package com.example.uade.rememberapp.ui.tags

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Tag
import com.example.uade.rememberapp.domain.repository.InvalidTagNameException
import com.example.uade.rememberapp.domain.repository.TagRepository
import com.example.uade.rememberapp.ui.components.DialogDismissButton
import com.example.uade.rememberapp.ui.components.DialogFilledConfirmButton
import com.example.uade.rememberapp.ui.components.HueSlider
import com.example.uade.rememberapp.ui.components.TagChip
import com.example.uade.rememberapp.ui.components.SectionLabel
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import java.util.Locale

/** Cuánto tiempo se puede deshacer un borrado. */
private const val UndoWindowMillis = 5_000L

/**
 * Panel de etiquetas del modal de nota rápida: asignar y quitar (tocando), crear, editar y
 * borrar (manteniendo presionada) etiquetas. Se despliega dentro del modal, como los paneles
 * de "Fecha y hora" y "Ubicación", en vez de abrir otra capa encima.
 *
 * Esta función es la "con estado": consigue el ViewModel, lo inicializa con [initialAssigned]
 * cada vez que aparece y dibuja [TagPickerContent]. Asignar y quitar se aplica al momento: cada
 * cambio de las asignadas (tocar, crear, borrar, deshacer) se avisa por [onAssignedChange].
 * [onCreatorExpandedChange] avisa cuando el editor (crear/editar) se despliega o se pliega.
 */
@Composable
fun TagPickerPanel(
    initialAssigned: Set<Long>,
    onAssignedChange: (Set<Long>) -> Unit,
    modifier: Modifier = Modifier,
    onCreatorExpandedChange: (Boolean) -> Unit = {},
    viewModel: TagPickerViewModel = viewModel(factory = TagPickerViewModel.Factory),
) {
    val currentOnAssignedChange by rememberUpdatedState(onAssignedChange)
    val currentOnCreatorExpandedChange by rememberUpdatedState(onCreatorExpandedChange)
    LaunchedEffect(viewModel) {
        viewModel.start(initialAssigned)
        // drop(1): la primera es la inicial, que quien abrió el panel ya conoce.
        viewModel.uiState
            .map { it.assignedIds }
            .distinctUntilChanged()
            .drop(1)
            .collect { currentOnAssignedChange(it) }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Quien contiene el panel puede necesitar saberlo, ej. para ocultar sus propios botones
    // mientras el editor muestra los suyos.
    LaunchedEffect(uiState.isCreatorExpanded) {
        currentOnCreatorExpandedChange(uiState.isCreatorExpanded)
    }

    TagPickerContent(
        uiState = uiState,
        actions = TagPickerActions(
            onTagClick = viewModel::onTagClick,
            onTagLongClick = viewModel::onTagLongClick,
            onNameChange = viewModel::onNameChange,
            onHueChange = viewModel::onHueChange,
            onSubmitEditor = viewModel::onSubmitEditor,
            onCancelEdit = viewModel::onCancelEdit,
            onToggleCreator = viewModel::onToggleCreator,
            onDeleteEditing = viewModel::onDeleteEditing,
            onUndoDelete = viewModel::onUndoDelete,
            onUndoTimeout = viewModel::onUndoTimeout,
            onCloseCreator = viewModel::onCloseCreator,
            onEditorDone = viewModel::onEditorDone,
        ),
        modifier = modifier,
    )
}

/** Todo lo que el usuario puede hacer en el panel (ver [TagPickerPanel]). */
data class TagPickerActions(
    val onTagClick: (id: Long) -> Unit = {},
    val onTagLongClick: (id: Long) -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onHueChange: (Float) -> Unit = {},
    val onSubmitEditor: () -> Unit = {},
    val onCancelEdit: () -> Unit = {},
    val onToggleCreator: () -> Unit = {},
    val onDeleteEditing: () -> Unit = {},
    val onUndoDelete: () -> Unit = {},
    val onUndoTimeout: () -> Unit = {},
    /** "Cancelar" del editor: lo pliega sin crear ni guardar. */
    val onCloseCreator: () -> Unit = {},
    /** "Hecho" del editor: crea o guarda lo escrito y lo pliega. */
    val onEditorDone: () -> Unit = {},
)

/**
 * Contenido del panel, sin estado ni marco (lo pone el modal que lo contiene). Sigue el mismo
 * formato que los paneles de "Fecha y hora" y "Ubicación": arranca directo con las secciones.
 * ```
 * ASIGNADAS    [● Salud]                ^ / ∨   ← pliega o despliega el editor
 * DISPONIBLES  [● Ideas]
 * ┌ solo con el editor desplegado ─────────────┐
 * [ Facultad            8/20 ] [ ● #4EE6E6 ]
 * (Editando «Ideas»   ✕  🗑  ✓)        ← solo al editar
 * [■■■■■■■■■■■■|■■■■■■■■■■■■]         ← tono
 *                  Cancelar  (Hecho)
 * └────────────────────────────────────────────┘
 * (Etiqueta eliminada · Deshacer)     ← solo tras borrar
 * ```
 * El editor arranca plegado: lo más común es asignar etiquetas que ya existen, y eso se hace
 * tocándolas, sin botones de confirmar.
 */
@Composable
fun TagPickerContent(
    uiState: TagPickerUiState,
    actions: TagPickerActions,
    modifier: Modifier = Modifier,
) {
    // El "Deshacer" se oculta solo pasado un rato; se reinicia con cada borrado.
    uiState.recentlyDeleted?.let { deleted ->
        LaunchedEffect(deleted) {
            delay(UndoWindowMillis)
            actions.onUndoTimeout()
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TagSection(
            title = stringResource(R.string.tags_section_assigned),
            tags = uiState.assigned,
            emptyText = stringResource(R.string.tags_empty_assigned),
            editingId = uiState.editingId,
            actions = actions,
            trailing = {
                CreatorToggleButton(
                    isCreatorExpanded = uiState.isCreatorExpanded,
                    onToggleCreator = actions.onToggleCreator,
                )
            },
        )
        TagSection(
            title = stringResource(R.string.tags_section_available),
            tags = uiState.available,
            emptyText = stringResource(R.string.tags_empty_available),
            editingId = uiState.editingId,
            actions = actions,
        )

        // Solo fundido: el alto lo anima el modal que contiene al panel (animateContentSize).
        // Si esto también animara el alto, las dos animaciones se pisarían.
        AnimatedVisibility(
            visible = uiState.isCreatorExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TagEditor(uiState = uiState, actions = actions)
                HueSlider(
                    hue = uiState.editorHue,
                    onHueChange = actions.onHueChange,
                    contentDescription = stringResource(R.string.tags_color),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DialogDismissButton(
                        text = stringResource(R.string.common_cancel),
                        onClick = actions.onCloseCreator,
                    )
                    DialogFilledConfirmButton(
                        text = stringResource(R.string.tags_done),
                        onClick = actions.onEditorDone,
                    )
                }
            }
        }

        AnimatedVisibility(visible = uiState.recentlyDeleted != null) {
            UndoBar(onUndo = actions.onUndoDelete)
        }
    }
}

/** Una sección ("ASIGNADAS" o "DISPONIBLES"); [trailing] va a la derecha del título. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSection(
    title: String,
    tags: List<Tag>,
    emptyText: String,
    editingId: Long?,
    actions: TagPickerActions,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(text = title, modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
        if (tags.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.forEach { tag ->
                    TagChip(
                        name = tag.name,
                        color = Color(tag.colorArgb),
                        onClick = { actions.onTagClick(tag.id) },
                        onLongClick = { actions.onTagLongClick(tag.id) },
                        highlighted = tag.id == editingId,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        textStyle = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

/** La flecha que pliega (∨) o despliega (^) el editor. */
@Composable
private fun CreatorToggleButton(
    isCreatorExpanded: Boolean,
    onToggleCreator: () -> Unit,
) {
    IconButton(onClick = onToggleCreator) {
        Icon(
            painter = painterResource(if (isCreatorExpanded) R.drawable.ic_expand_more else R.drawable.ic_expand_less),
            contentDescription = stringResource(
                if (isCreatorExpanded) R.string.tags_hide_creator else R.string.tags_show_creator,
            ),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}


/** Campo del nombre + chip del color; en modo edición suma la fila de acciones. */
@Composable
private fun TagEditor(
    uiState: TagPickerUiState,
    actions: TagPickerActions,
) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NameField(
                name = uiState.editorName,
                onNameChange = actions.onNameChange,
                onSubmit = actions.onSubmitEditor,
                isError = uiState.error != null,
                showAddButton = !uiState.isEditing && uiState.editorName.isNotBlank(),
                modifier = Modifier.weight(1f),
            )
            ColorValueChip(colorArgb = uiState.editorColorArgb)
        }

        uiState.error?.let { reason ->
            Text(
                text = stringResource(reason.message()),
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
            )
        }

        uiState.editingTag?.let { editing ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.tags_editing, editing.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = actions.onCancelEdit) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.tags_cancel_edit),
                        tint = colors.onSurfaceVariant,
                    )
                }
                IconButton(onClick = actions.onDeleteEditing) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.tags_delete),
                        tint = colors.error,
                    )
                }
                IconButton(onClick = actions.onSubmitEditor) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = stringResource(R.string.tags_save),
                        tint = colors.primary,
                    )
                }
            }
        }
    }
}

/** Campo con borde, hint "Nueva etiqueta" y contador "8/20". */
@Composable
private fun NameField(
    name: String,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isError: Boolean,
    showAddButton: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface)
    Surface(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, if (isError) colors.error else colors.outline),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = if (showAddButton) 4.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.weight(1f),
                textStyle = textStyle,
                singleLine = true,
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                decorationBox = { inner ->
                    Box {
                        if (name.isEmpty()) {
                            Text(
                                text = stringResource(R.string.tags_name_hint),
                                style = textStyle,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    }
                },
            )
            Text(
                text = stringResource(R.string.tags_name_counter, name.length, TagRepository.MaxNameLength),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
            if (showAddButton) {
                IconButton(onClick = onSubmit) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.tags_add),
                        tint = colors.primary,
                    )
                }
            }
        }
    }
}

/** Chip con el color elegido: punto + hex, ej. "● #4EE6E6". */
@Composable
private fun ColorValueChip(colorArgb: Long) {
    Row(
        modifier = Modifier
            .height(56.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(colorArgb)),
        )
        Text(
            text = String.format(Locale.ROOT, "#%06X", colorArgb and 0xFFFFFF),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** "Etiqueta eliminada · Deshacer". */
@Composable
private fun UndoBar(onUndo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.tags_deleted),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onUndo) {
            Text(
                text = stringResource(R.string.tags_undo),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.size(4.dp))
    }
}

/** Texto para cada motivo de nombre inválido. */
private fun InvalidTagNameException.Reason.message(): Int = when (this) {
    InvalidTagNameException.Reason.Empty -> R.string.tags_error_empty
    InvalidTagNameException.Reason.TooLong -> R.string.tags_error_too_long
    InvalidTagNameException.Reason.Duplicate -> R.string.tags_error_duplicate
}

private val PreviewTags = listOf(
    Tag(id = 1, name = "Salud", colorArgb = 0xFF6FCF97),
    Tag(id = 5, name = "Ideas", colorArgb = 0xFFF2C94C),
    Tag(id = 7, name = "Facultad", colorArgb = 0xFF50E6E6),
)

/** Estado de ejemplo del panel, para previews (también las del modal de nota rápida). */
internal fun previewTagPickerState(): TagPickerUiState = TagPickerUiState(
    tags = PreviewTags,
    assignedIds = setOf(1),
)

@Composable
private fun TagPickerPreviewFrame(uiState: TagPickerUiState) {
    RememberAppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            TagPickerContent(
                uiState = uiState,
                actions = TagPickerActions(),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "Plegada (inicial)")
@Composable
private fun TagPickerContentPreview() {
    TagPickerPreviewFrame(previewTagPickerState())
}

@Preview(name = "Desplegada")
@Composable
private fun TagPickerContentExpandedPreview() {
    TagPickerPreviewFrame(previewTagPickerState().copy(isCreatorExpanded = true, editorName = "Trabajo"))
}

@Preview(name = "Editando")
@Composable
private fun TagPickerContentEditingPreview() {
    TagPickerPreviewFrame(
        previewTagPickerState().copy(
            isCreatorExpanded = true,
            editingId = 7,
            editorName = "Facultad",
            editorColorArgb = 0xFF50E6E6,
            editorHue = 180f,
        ),
    )
}

@Preview(name = "Deshacer y error")
@Composable
private fun TagPickerContentUndoPreview() {
    TagPickerPreviewFrame(
        TagPickerUiState(
            tags = PreviewTags.drop(1),
            isCreatorExpanded = true,
            editorName = "Ideas",
            error = InvalidTagNameException.Reason.Duplicate,
            recentlyDeleted = DeletedTag(PreviewTags.first(), wasAssigned = true),
        ),
    )
}
