package com.example.uade.rememberapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/** Saturación y brillo fijos: así cualquier tono queda legible sobre el fondo oscuro. */
private const val TagSaturation = 0.65f
private const val TagValue = 0.9f

/** Color de etiqueta para un tono (0–360). */
fun tagColorForHue(hue: Float): Color = Color.hsv(hue.coerceIn(0f, 360f) % 360f, TagSaturation, TagValue)

/** Tono (0–360) de un color, para ubicar el cursor de [HueSlider]. */
fun hueOf(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv[0]
}

private val HueStops: List<Color> = (0..12).map { tagColorForHue(it * 30f) }

/**
 * Barra arcoíris para elegir un tono: tocarla o arrastrar el cursor avisa el tono (0–360)
 * por [onHueChange]. No guarda estado: el tono actual entra por [hue].
 */
@Composable
fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val currentOnHueChange by rememberUpdatedState(onHueChange)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(HueStops))
            .pointerInput(Unit) {
                detectTapGestures { offset -> currentOnHueChange(offset.x / size.width * 360f) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    currentOnHueChange((change.position.x / size.width * 360f).coerceIn(0f, 360f))
                }
            }
            // TalkBack: se anuncia como un control ajustable de 0 a 360.
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(hue, 0f..360f)
                setProgress { value ->
                    currentOnHueChange(value.coerceIn(0f, 360f))
                    true
                }
            },
    ) {
        val thumbWidth = 6.dp
        val position = (maxWidth - thumbWidth) * (hue.coerceIn(0f, 360f) / 360f)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = position)
                .padding(vertical = 2.dp)
                .width(thumbWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(50)),
        )
    }
}

@Preview
@Composable
private fun HueSliderPreview() {
    RememberAppTheme {
        HueSlider(hue = 180f, onHueChange = {}, contentDescription = "Color", modifier = Modifier.padding(16.dp))
    }
}
