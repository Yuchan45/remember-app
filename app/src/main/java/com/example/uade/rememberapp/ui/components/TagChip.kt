package com.example.uade.rememberapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Etiqueta: punto de color + nombre, ej. "● Salud".
 *
 * Recibe nombre y color sueltos (no un Tag del dominio) para poder usarse también en
 * filtros o en el editor de etiquetas.
 *
 * Por defecto es solo visual (como en las cards). Con [onClick] y/o [onLongClick] se puede
 * tocar y mantener presionada, y [highlighted] le dibuja un borde celeste (ej. la etiqueta que
 * se está editando).
 */
@Composable
fun TagChip(
    name: String,
    color: Color,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
    textStyle: TextStyle = MaterialTheme.typography.labelMedium,
) {
    val shape = RoundedCornerShape(50)
    val interactive = onClick != null || onLongClick != null
    Row(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .then(if (highlighted) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .then(
                if (interactive) {
                    Modifier.combinedClickable(
                        role = Role.Button,
                        onClick = onClick ?: {},
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = name,
            style = textStyle,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Composable
private fun TagChipPreview() {
    RememberAppTheme {
        TagChip(
            name = "Salud",
            color = Color(0xFF6FCF97),
            modifier = Modifier.padding(8.dp),
        )
    }
}
