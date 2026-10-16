package com.example.uade.rememberapp.ui.reminders.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.zIndex
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.reminders.components.ReminderPhotoBackground
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/** Cuántas fotos se ven en la pila; el resto se resume en "+n". */
private const val MaxVisible = 5

/** Alto de la pila. Menos que la portada sola a todo el ancho, para dejar más lugar al texto. */
private val StackHeight = 124.dp

/** Lo mínimo y lo máximo que asoma cada foto de atrás. */
private val MinPeek = 16.dp
private val MaxPeek = 40.dp

private val CardShape = RoundedCornerShape(16.dp)

/**
 * Las fotos del recordatorio apiladas de izquierda a derecha, arriba de la descripción:
 * ```
 * ┌───────────────────┐┐┐┐┐
 * │  portada (16:9)   │││││   ← adelante la primera; de las otras asoma una franja
 * │                   ││││+7  ← "+n": las que no entran en la pila
 * └───────────────────┘┘┘┘┘
 * ```
 * La de adelante mantiene la proporción [aspectRatio] (la del recorte), así la portada se ve
 * tal cual se recortó. Las de atrás se centran y recortan solas. Tocar la pila abre el modal.
 *
 * [photoPaths] van en orden: la primera queda adelante (ver ReminderDetailUiState.stackPhotos).
 */
@Composable
fun PhotoStack(
    photoPaths: List<String>,
    aspectRatio: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = photoPaths.take(MaxVisible)
    val hidden = photoPaths.size - visible.size
    // Toda la pila se lee como un solo elemento: "3 fotos", y se puede tocar.
    val description = pluralStringResource(R.plurals.reminder_detail_image_count, photoPaths.size, photoPaths.size)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            },
    ) {
        val extra = visible.size - 1
        // El alto se achica solo si el ancho no alcanza para la de adelante y las franjas mínimas.
        val height = min(StackHeight, (maxWidth - MinPeek * extra) / aspectRatio)
        val cardWidth = height * aspectRatio
        val peek: Dp = if (extra > 0) ((maxWidth - cardWidth) / extra).coerceIn(MinPeek, MaxPeek) else 0.dp

        // Del ancho justo de la pila: así el "+n" se alinea abajo a la derecha de la última franja.
        Box(modifier = Modifier.size(width = cardWidth + peek * extra, height = height)) {
            visible.forEachIndexed { index, path ->
                StackCard(
                    photoPath = path,
                    modifier = Modifier
                        .offset(x = peek * index)
                        .size(width = cardWidth, height = height)
                        // La primera arriba de todo; cada una de las siguientes, un poco más atrás.
                        .zIndex((visible.size - index).toFloat()),
                )
            }
            if (hidden > 0) {
                MoreBadge(
                    count = hidden,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .zIndex(visible.size + 1f),
                )
            }
        }
    }
}

/** Una foto de la pila. El borde la separa de la de atrás, como un marco. */
@Composable
private fun StackCard(photoPath: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(3.dp, MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        ReminderPhotoBackground(
            photoPath = photoPath,
            scrim = false,
            modifier = Modifier
                .fillMaxSize()
                .clip(CardShape),
        )
    }
}

/** "+7": las fotos que no entran en la pila, en una pastilla oscura para leerse sobre cualquier foto. */
@Composable
private fun MoreBadge(count: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.6f),
        contentColor = Color.White,
    ) {
        Text(
            text = stringResource(R.string.reminder_detail_photos_more, count),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Preview(name = "12 fotos (se ven 5, +7)", showBackground = true, backgroundColor = 0xFF0B141B, widthDp = 360)
@Composable
private fun PhotoStackManyPreview() {
    RememberAppTheme {
        // Rutas inventadas: las previews no cargan archivos y muestran el degradé de fondo.
        PhotoStack(
            photoPaths = List(12) { "foto$it.jpg" },
            aspectRatio = 16f / 9f,
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Una sola foto", showBackground = true, backgroundColor = 0xFF0B141B, widthDp = 360)
@Composable
private fun PhotoStackSinglePreview() {
    RememberAppTheme {
        PhotoStack(
            photoPaths = listOf("portada.jpg"),
            aspectRatio = 16f / 9f,
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
