package com.example.uade.rememberapp.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.CropRect
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import java.io.File
import kotlin.math.max

/** Cuánto se puede acercar, sobre el tamaño en que la foto justo cubre el marco. */
private const val MaxZoom = 5f

/** Cuánto acercan o alejan los botones − y + en cada toque. */
private const val ZoomStep = 1.25f

/** Margen entre el marco y los bordes de la pantalla. */
private val FrameMargin = 16.dp

/** Esquinas del marco: las mismas que la portada en el detalle. */
private val FrameCorner = 20.dp

/**
 * Pantalla completa para elegir qué porción de la foto en [imagePath] se usa:
 * ```
 * ✕
 * ░░░░░░░░░░░░░░░░░░░░░░░░░░░░   ← la foto, oscurecida fuera del marco
 * ┌──────────────────────────┐
 * │   (lo que queda adentro) │   ← marco fijo de proporción [aspectRatio]
 * └──────────────────────────┘
 * ░░░░░░░░░░░░░░░░░░░░░░░░░░░░
 * (✕ Cancelar)  (−)  (+)  (✓ Guardar)
 * ```
 * El marco no se mueve: se mueve y se agranda la foto debajo (arrastrando y pellizcando, o con
 * − y +). Así siempre se ve exactamente cómo va a quedar.
 *
 * "Guardar" devuelve el recorte como [CropRect] (proporciones de la foto, no píxeles).
 */
@Composable
fun ImageCropDialog(
    imagePath: String,
    onConfirm: (CropRect) -> Unit,
    onDismiss: () -> Unit,
    aspectRatio: Float = 16f / 9f,
) {
    val context = LocalPlatformContext.current
    // Tamaño fijo: así Coil la carga enseguida (sin esperar a saber dónde se dibuja) y no carga
    // más píxeles de los que entran en pantalla. Mantiene la proporción de la foto.
    val request = remember(imagePath) {
        ImageRequest.Builder(context).data(File(imagePath)).size(2048).build()
    }
    val painter = rememberAsyncImagePainter(request)
    val state by painter.state.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        // Pantalla completa, también debajo de las barras del sistema.
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val image = (state as? AsyncImagePainter.State.Success)?.result?.image
        ImageCropContent(
            painter = painter,
            imageSize = image?.let { IntSize(it.width, it.height) },
            aspectRatio = aspectRatio,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}

/**
 * La pantalla de recorte sin estado de carga: recibe la foto ya lista como [painter] y su
 * tamaño ([imageSize]; null = todavía cargando). Solo guarda el zoom y el desplazamiento.
 */
@Composable
private fun ImageCropContent(
    painter: Painter,
    imageSize: IntSize?,
    aspectRatio: Float,
    onConfirm: (CropRect) -> Unit,
    onDismiss: () -> Unit,
) {
    // zoom 1 = la foto justo cubre el marco. offset = cuánto se corrió el centro de la foto
    // respecto del centro del marco, en píxeles de pantalla.
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        IconButton(onClick = onDismiss, modifier = Modifier.padding(4.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.image_crop_close),
                tint = Color.White,
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds(),
            contentAlignment = Alignment.Center,
        ) {
            val density = LocalDensity.current
            val area = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
            val frame = frameSize(area, aspectRatio, with(density) { FrameMargin.toPx() })

            if (imageSize == null) {
                CircularProgressIndicator(color = Color.White)
            } else {
                // Escala con la que la foto justo cubre el marco (el lado que menos sobra, ajusta).
                val baseScale = max(frame.width / imageSize.width, frame.height / imageSize.height)
                val geometry = CropGeometry(imageSize, frame, baseScale)

                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        // requiredSize: puede ser más grande que el área (se recorta afuera).
                        .requiredSize(
                            with(density) { (imageSize.width * baseScale).toDp() },
                            with(density) { (imageSize.height * baseScale).toDp() },
                        )
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = offset.x
                            translationY = offset.y
                        },
                )

                FrameOverlay(frame = frame, modifier = Modifier.fillMaxSize())

                // Los gestos van en una capa encima de todo y se reinician si cambia el tamaño.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(geometry) {
                            detectTransformGestures { _, pan, zoomChange, _ ->
                                zoom = (zoom * zoomChange).coerceIn(1f, MaxZoom)
                                offset = geometry.clamp(offset + pan, zoom)
                            }
                        },
                )

                CropBottomBar(
                    onCancel = onDismiss,
                    onZoomOut = {
                        zoom = (zoom / ZoomStep).coerceAtLeast(1f)
                        offset = geometry.clamp(offset, zoom)
                    },
                    onZoomIn = {
                        zoom = (zoom * ZoomStep).coerceAtMost(MaxZoom)
                        offset = geometry.clamp(offset, zoom)
                    },
                    onSave = { onConfirm(geometry.cropRect(offset, zoom)) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}

/** El marco más grande de proporción [aspectRatio] que entra en [area] con [margin] a los lados. */
private fun frameSize(area: Size, aspectRatio: Float, margin: Float): Size {
    val width = area.width - margin * 2
    val height = width / aspectRatio
    // En pantallas muy apaisadas el alto manda: el marco no ocupa más del 70% del alto.
    val maxHeight = area.height * 0.7f
    return if (height <= maxHeight) Size(width, height) else Size(maxHeight * aspectRatio, maxHeight)
}

/**
 * Las cuentas del recorte. La foto se dibuja centrada en el marco, con tamaño
 * `imageSize * baseScale * zoom` y corrida `offset`.
 */
private data class CropGeometry(
    val imageSize: IntSize,
    val frame: Size,
    val baseScale: Float,
) {
    private fun shown(zoom: Float) =
        Size(imageSize.width * baseScale * zoom, imageSize.height * baseScale * zoom)

    /**
     * Limita el desplazamiento para que la foto siempre cubra el marco: el borde de la foto no
     * puede entrar al marco. Lo que sobra de cada lado es (foto − marco) / 2.
     */
    fun clamp(offset: Offset, zoom: Float): Offset {
        val shown = shown(zoom)
        val maxX = (shown.width - frame.width) / 2
        val maxY = (shown.height - frame.height) / 2
        return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
    }

    /**
     * Qué parte de la foto queda dentro del marco, en proporciones. El borde izquierdo del marco
     * está a `(foto − marco) / 2 − offset` del borde izquierdo de la foto (igual arriba).
     */
    fun cropRect(offset: Offset, zoom: Float): CropRect {
        val shown = shown(zoom)
        val left = ((shown.width - frame.width) / 2 - offset.x) / shown.width
        val top = ((shown.height - frame.height) / 2 - offset.y) / shown.height
        val width = frame.width / shown.width
        val height = frame.height / shown.height
        return CropRect(
            left = left.coerceIn(0f, 1f - width),
            top = top.coerceIn(0f, 1f - height),
            right = (left + width).coerceIn(width, 1f),
            bottom = (top + height).coerceIn(height, 1f),
        )
    }
}

/** Oscurece todo menos el marco y le dibuja un borde fino. */
@Composable
private fun FrameOverlay(frame: Size, modifier: Modifier = Modifier) {
    val corner = with(LocalDensity.current) { FrameCorner.toPx() }
    Canvas(modifier = modifier) {
        val rect = Rect(
            offset = Offset((size.width - frame.width) / 2, (size.height - frame.height) / 2),
            size = frame,
        )
        val hole = Path().apply { addRoundRect(RoundRect(rect, CornerRadius(corner))) }
        // ClipOp.Difference: pinta en todo el lienzo salvo dentro del marco.
        clipPath(hole, clipOp = ClipOp.Difference) {
            drawRect(Color.Black.copy(alpha = 0.6f))
        }
        drawPath(hole, color = Color.White.copy(alpha = 0.8f), style = Stroke(width = 1.dp.toPx()))
    }
}

/** "Cancelar", −, + y "Guardar", en pastillas oscuras sobre la foto. */
@Composable
private fun CropBottomBar(
    onCancel: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CropBarButton(R.drawable.ic_close, stringResource(R.string.common_cancel), showText = true, onClick = onCancel)
        CropBarButton(R.drawable.ic_zoom_out, stringResource(R.string.image_crop_zoom_out), showText = false, onClick = onZoomOut)
        CropBarButton(R.drawable.ic_zoom_in, stringResource(R.string.image_crop_zoom_in), showText = false, onClick = onZoomIn)
        CropBarButton(R.drawable.ic_check, stringResource(R.string.image_crop_save), showText = true, onClick = onSave)
    }
}

/** Una pastilla: ícono solo (con [text] para lectores de pantalla) o ícono y texto. */
@Composable
private fun CropBarButton(
    @DrawableRes icon: Int,
    text: String,
    showText: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color(0xFF2B2B2B),
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (showText) 14.dp else 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = if (showText) null else text,
                modifier = Modifier.size(18.dp),
            )
            if (showText) Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun ImageCropContentPreview() {
    RememberAppTheme {
        ImageCropContent(
            // Un rectángulo de color en lugar de la foto (las previews no cargan archivos).
            painter = ColorPainter(Color(0xFF3E8E7E)),
            imageSize = IntSize(1600, 1200),
            aspectRatio = 16f / 9f,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
