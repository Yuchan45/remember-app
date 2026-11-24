package com.example.uade.rememberapp.ui.reminders.capture.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import java.io.File

/**
 * Las fotos adjuntas en la nota rápida, en una fila que se desplaza de costado:
 * ```
 * [foto ✕] [foto ✕] [foto ✕] (⟳ mientras se copian)
 * ```
 * La primera es la que va a quedar de portada. La ✕ de cada una la saca de la nota.
 *
 * Sin estado: recibe las rutas y avisa por [onRemove].
 */
@Composable
fun CapturePhotosRow(
    photoPaths: List<String>,
    isImporting: Boolean,
    onRemove: (path: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        // El margen va como contentPadding: así las fotos se desplazan hasta el borde del modal.
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(items = photoPaths, key = { _, path -> path }) { index, path ->
            Box(modifier = Modifier.size(72.dp)) {
                AsyncImage(
                    // File y no el String: así Coil no intenta interpretarlo como URL.
                    model = File(path),
                    contentDescription = stringResource(R.string.reminder_photos_item, index + 1),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
                // ✕ arriba a la derecha, sobre un círculo oscuro para verse sobre cualquier foto.
                IconButton(
                    onClick = { onRemove(path) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.reminders_capture_remove_photo),
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        if (isImporting) {
            item(key = "importing") {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E2C36)
@Composable
private fun CapturePhotosRowPreview() {
    RememberAppTheme {
        // Rutas inventadas: las previews no cargan archivos y muestran el fondo.
        CapturePhotosRow(
            photoPaths = listOf("a.jpg", "b.jpg"),
            isImporting = true,
            onRemove = {},
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}
