package com.example.uade.rememberapp.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Cómo se marca un [OptionChip] seleccionado. Sirve para distinguir jerarquías: las opciones
 * principales van con fondo y las secundarias (las que están dentro de un panel) solo con borde.
 */
enum class OptionChipStyle {
    /** Fondo de acento (primaryContainer). Para opciones de más jerarquía. */
    Filled,

    /** Solo el borde y el texto en color de acento, sin fondo. Para opciones secundarias. */
    Outlined,
}

/**
 * Chip de opción, con ícono opcional, ej. "☀ Mañana 9:00".
 *
 * - No seleccionado: borde gris y fondo transparente.
 * - Seleccionado: según [style], con fondo de acento o solo con borde de acento.
 *
 * No guarda estado: [selected] entra por parámetro y el toque sale por [onClick].
 * [contentPadding] e [iconSpacing] permiten una versión más compacta donde el ancho es justo
 * (ej. la fila de avisos de la nota rápida).
 *
 * - Con [description], debajo del texto va una explicación más chica en varias líneas, ej. los
 *   niveles de "Importancia". Conviene usarlo a lo ancho (`fillMaxWidth`).
 * - Con [showText] en false se ve solo el ícono, y el texto pasa a ser su descripción para
 *   accesibilidad. Al cambiar, el chip se agranda o achica animado, como los destinos de la
 *   barra inferior.
 */
@Composable
fun OptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    style: OptionChipStyle = OptionChipStyle.Filled,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
    iconSpacing: Dp = 8.dp,
    description: String? = null,
    showText: Boolean = true,
) {
    // Sin ícono no hay con qué reemplazar el texto: se muestra igual.
    val textVisible = showText || icon == null
    val colors = MaterialTheme.colorScheme
    val filled = selected && style == OptionChipStyle.Filled
    val outlined = selected && style == OptionChipStyle.Outlined
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
        shape = RoundedCornerShape(12.dp),
        color = if (filled) colors.primaryContainer else Color.Transparent,
        contentColor = when {
            filled -> colors.onPrimaryContainer
            outlined -> colors.primary
            else -> colors.onSurface
        },
        border = when {
            filled -> null
            outlined -> BorderStroke(1.5.dp, colors.primary)
            else -> BorderStroke(1.dp, colors.outline)
        },
    ) {
        Row(
            modifier = Modifier
                .animateContentSize()
                .padding(contentPadding),
            // Con descripción el ícono acompaña al título (arriba), no al centro del bloque.
            verticalAlignment = if (description != null) Alignment.Top else Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(iconSpacing),
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = if (textVisible) null else text,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (textVisible) {
                // Sin weight a propósito: dentro de un LazyRow (ancho ilimitado) un hijo con weight
                // mide 0 y el texto desaparece. El Row ya le pasa el ancho que queda libre, así que
                // la descripción igual se parte en líneas cuando el chip va a lo ancho.
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            // Seleccionado toma el color de acento del chip; si no, gris.
                            color = if (selected) LocalContentColor.current else colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E2C36)
@Composable
private fun OptionChipPreview() {
    RememberAppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OptionChip(text = "En 1 hora", icon = R.drawable.ic_timer, selected = false, onClick = {})
            OptionChip(text = "Fecha y hora", icon = R.drawable.ic_schedule, selected = true, onClick = {})
            OptionChip(
                text = "Mañana 9:00",
                icon = R.drawable.ic_light_mode,
                selected = true,
                onClick = {},
                style = OptionChipStyle.Outlined,
            )
            OptionChip(text = "Diario", selected = false, onClick = {})
            // Solo ícono (no seleccionado) y expandido (seleccionado), como la fila de avisos.
            OptionChip(text = "Ubicación", icon = R.drawable.ic_location_on, selected = false, onClick = {}, showText = false)
            OptionChip(
                text = "Alta",
                icon = R.drawable.ic_notifications_active,
                selected = true,
                onClick = {},
                style = OptionChipStyle.Outlined,
                description = "Aparece en pantalla como alerta emergente",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
