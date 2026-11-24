package com.example.uade.rememberapp.ui.reminders.detail.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.MAX_PHOTOS
import com.example.uade.rememberapp.domain.model.ReminderPhoto
import com.example.uade.rememberapp.ui.components.DialogDismissButton
import com.example.uade.rememberapp.ui.components.DialogFilledConfirmButton
import com.example.uade.rememberapp.ui.reminders.detail.PhotosEditor
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import java.io.File

/** Casilleros por fila en la grilla del modal. */
private const val GridColumns = 4

/** Alto máximo de la grilla: con muchas fotos se desplaza en vez de tapar toda la pantalla. */
private val GridMaxHeight = 360.dp

/** Todo lo que se puede hacer en el modal "Imágenes" (ver [PhotosSheet]). */
data class PhotosSheetActions(
    val onAdd: () -> Unit = {},
    val onPhotoClick: (path: String) -> Unit = {},
    val onRemove: (path: String) -> Unit = {},
    val onCancel: () -> Unit = {},
    val onSave: () -> Unit = {},
)

/** Modal "Imágenes". Cerrarlo deslizando o tocando afuera es lo mismo que "Cancelar". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosSheet(
    editor: PhotosEditor,
    actions: PhotosSheetActions,
) {
    ModalBottomSheet(
        onDismissRequest = actions.onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        PhotosSheetContent(
            editor = editor,
            actions = actions,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

/**
 * ```
 * Imágenes
 * Elegí hasta 15 fotos y tocá una para usarla de portada.
 * ┌────✕┐ ┌────✕┐ ┌────✕┐ ┌────✕┐
 * │ ✓   │ │     │ │     │ │     │
 * └─────┘ └─────┘ └─────┘ └─────┘
 * ┌────✕┐ ┌ ─ ─ ┐
 * │     │   🖼+
 * └─────┘ └ ─ ─ ┘
 *                      Cancelar  (Guardar)
 * ```
 * Una grilla de [GridColumns] columnas: las fotos y, mientras haya lugar, un casillero que abre
 * el selector de Android. Si no entra, la grilla se desplaza.
 */
@Composable
fun PhotosSheetContent(
    editor: PhotosEditor,
    actions: PhotosSheetActions,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.reminder_photos_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
            Text(
                text = if (editor.isImporting) {
                    stringResource(R.string.reminder_photos_loading)
                } else {
                    pluralStringResource(R.plurals.reminder_photos_subtitle, MAX_PHOTOS, MAX_PHOTOS)
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        // Las fotos con su número (para lectores de pantalla) y, si hay lugar, el casillero de agregar.
        val cells: List<IndexedValue<ReminderPhoto>?> =
            editor.photos.withIndex().toList() + if (editor.freeSlots > 0) listOf(null) else emptyList()
        Column(
            modifier = Modifier
                .heightIn(max = GridMaxHeight)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Filas de GridColumns casilleros del mismo ancho; la última se completa con espacios.
            cells.chunked(GridColumns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val slotModifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                    row.forEach { cell ->
                        if (cell != null) {
                            val photo = cell.value
                            PhotoSlot(
                                photo = photo,
                                number = cell.index + 1,
                                onClick = { actions.onPhotoClick(photo.path) },
                                onRemove = { actions.onRemove(photo.path) },
                                modifier = slotModifier,
                            )
                        } else {
                            AddSlot(isLoading = editor.isImporting, onClick = actions.onAdd, modifier = slotModifier)
                        }
                    }
                    repeat(GridColumns - row.size) { Spacer(slotModifier) }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DialogDismissButton(text = stringResource(R.string.common_cancel), onClick = actions.onCancel)
            DialogFilledConfirmButton(
                text = stringResource(R.string.reminder_alarms_save),
                onClick = actions.onSave,
                // Esperar a que terminen de copiarse: si no, las que se están importando se pierden.
                enabled = !editor.isImporting,
            )
        }
    }
}

/** Una foto elegida: tocarla abre el recorte; ✕ la quita. La portada lleva borde y un ✓. */
@Composable
private fun PhotoSlot(
    photo: ReminderPhoto,
    number: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier,
        shape = shape,
        color = colors.surfaceContainerHigh,
        border = if (photo.isCover) BorderStroke(2.dp, colors.primary) else null,
    ) {
        Box {
            AsyncImage(
                model = File(photo.path),
                contentDescription = stringResource(R.string.reminder_photos_item, number),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .clickable(onClick = onClick),
            )
            // ✕ arriba a la derecha, sobre un círculo oscuro para que se vea en cualquier foto.
            SlotBadge(
                icon = R.drawable.ic_close,
                contentDescription = stringResource(R.string.reminder_photos_remove),
                background = Color.Black.copy(alpha = 0.55f),
                tint = Color.White,
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            )
            if (photo.isCover) {
                // Con 4 por fila no entra "Portada" escrito: va el ✓ (y el texto para lectores de pantalla).
                SlotBadge(
                    icon = R.drawable.ic_check,
                    contentDescription = stringResource(R.string.reminder_photos_cover),
                    background = colors.primary,
                    tint = colors.onPrimary,
                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
                )
            }
        }
    }
}

/** Un ícono chico en un círculo, sobre la foto. Con [onClick] es un botón. */
@Composable
private fun SlotBadge(
    @DrawableRes icon: Int,
    contentDescription: String,
    background: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
    }
}

/** El casillero para agregar más fotos: tocarlo abre el selector de Android. */
@Composable
private fun AddSlot(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_add_photo_alternate),
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(R.string.reminder_photos_add),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(name = "Sin fotos", showBackground = true, backgroundColor = 0xFF1E2C36)
@Composable
private fun PhotosSheetEmptyPreview() {
    RememberAppTheme {
        PhotosSheetContent(editor = PhotosEditor(), actions = PhotosSheetActions())
    }
}

@Preview(name = "Dos fotos, una de portada", showBackground = true, backgroundColor = 0xFF1E2C36)
@Composable
private fun PhotosSheetWithPhotosPreview() {
    RememberAppTheme {
        PhotosSheetContent(
            editor = PhotosEditor(
                photos = listOf(ReminderPhoto("a.jpg", isCover = true), ReminderPhoto("b.jpg")),
                coverPath = "portada.jpg",
            ),
            actions = PhotosSheetActions(),
        )
    }
}
