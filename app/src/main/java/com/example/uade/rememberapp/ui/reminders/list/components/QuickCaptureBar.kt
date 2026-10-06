package com.example.uade.rememberapp.ui.reminders.list.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Barra de captura rápida: "Nota rápida…  🎤 🖼". El texto ocupa todo el ancho disponible;
 * el botón "+" ya no está acá sino al lado de la barra de navegación (ver MainTabs).
 *
 * Es un botón con forma de campo: tocarla avisa por [onClick] y deslizarla hacia arriba avisa
 * por [onSwipeUp] (en la Home, los dos abren el modal de captura rápida).
 */
@Composable
fun QuickCaptureBar(
    onClick: () -> Unit,
    onVoice: () -> Unit,
    onAddPhoto: () -> Unit,
    modifier: Modifier = Modifier,
    onSwipeUp: () -> Unit = onClick,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .swipeUpToOpen(onSwipeUp),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 6.dp,
    ) {
        Row(
            // Sin el "+", el texto arranca con el mismo margen que la píldora de la barra.
            modifier = Modifier.padding(start = 20.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.reminders_quick_capture_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onVoice) {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = stringResource(R.string.reminders_quick_capture_voice),
                )
            }
            IconButton(onClick = onAddPhoto) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_photo_alternate),
                    contentDescription = stringResource(R.string.reminders_quick_capture_photo),
                )
            }
        }
    }
}

/** Distancia mínima hacia arriba para contar como swipe (evita abrir con un roce). */
private val SwipeThreshold = 24.dp

/** Velocidad hacia arriba (px/s) a partir de la cual un swipe corto igual cuenta. */
private const val SwipeVelocityThreshold = 800f

/**
 * Detecta un deslizamiento hacia arriba sobre la barra: avisa por [onSwipeUp] si el dedo subió
 * al menos [SwipeThreshold], o si fue un gesto rápido. Un toque sigue siendo un click normal.
 */
@Composable
private fun Modifier.swipeUpToOpen(onSwipeUp: () -> Unit): Modifier {
    val thresholdPx = with(LocalDensity.current) { SwipeThreshold.toPx() }
    val currentOnSwipeUp by rememberUpdatedState(onSwipeUp)
    // Recorrido acumulado del gesto actual; negativo = hacia arriba.
    var dragged by remember { mutableFloatStateOf(0f) }

    return draggable(
        orientation = Orientation.Vertical,
        state = rememberDraggableState { delta -> dragged += delta },
        onDragStarted = { dragged = 0f },
        onDragStopped = { velocity ->
            if (dragged < -thresholdPx || velocity < -SwipeVelocityThreshold) {
                currentOnSwipeUp()
            }
            dragged = 0f
        },
    )
}

@Preview
@Composable
private fun QuickCaptureBarPreview() {
    RememberAppTheme {
        QuickCaptureBar(
            onClick = {},
            onVoice = {},
            onAddPhoto = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
