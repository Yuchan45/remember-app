package com.example.uade.rememberapp.ui.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.AppPalette
import com.example.uade.rememberapp.ui.theme.AvailablePalettes
import com.example.uade.rememberapp.ui.theme.LocalIsDarkTheme
import com.example.uade.rememberapp.ui.theme.PaletteInfo
import com.example.uade.rememberapp.ui.theme.getPaletteSwatchColors

/**
 * Card de selección de paleta de colores y vista previa de tonos primario, secundario,
 * terciario y superficies, adaptados en tiempo real al tema claro u oscuro del sistema.
 */
@Composable
fun PaletteSelectorCard(
    selectedPalette: AppPalette,
    onPaletteSelected: (AppPalette) -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalIsDarkTheme.current,
) {
    val activeInfo = AvailablePalettes.firstOrNull { it.palette == selectedPalette }
        ?: AvailablePalettes.first { it.palette == AppPalette.HeyBlue }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_palette_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Fila de círculos de paleta
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvailablePalettes.forEach { info ->
                    PaletteSwatchCircle(
                        info = info,
                        isDarkTheme = isDarkTheme,
                        isSelected = info.palette == selectedPalette,
                        onClick = { onPaletteSelected(info.palette) },
                    )
                }
            }

            // Vista previa de la paleta
            PalettePreviewBlock(info = activeInfo)
        }
    }
}

@Composable
private fun PaletteSwatchCircle(
    info: PaletteInfo,
    isDarkTheme: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ringColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = modifier
            .size(54.dp)
            .border(2.dp, ringColor, CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (info.palette == AppPalette.Dynamic) {
            // Material You: ícono de paleta en el centro
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_palette),
                    contentDescription = stringResource(info.nameRes),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            // Círculo dividido en 3 o 4 cuadrantes con los colores adaptados a claro u oscuro
            val colors = getPaletteSwatchColors(info.palette, isDarkTheme)
            Canvas(modifier = Modifier.size(46.dp)) {
                val count = colors.size
                val sweep = 360f / count
                colors.forEachIndexed { i, color ->
                    drawArc(
                        color = color,
                        startAngle = i * sweep,
                        sweepAngle = sweep,
                        useCenter = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun PalettePreviewBlock(
    info: PaletteInfo,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_palette_preview_title, stringResource(info.nameRes)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )

        // 3 Cards de colores: primario, secundario, terciario
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ColorPreviewTile(
                title = stringResource(R.string.settings_palette_primary),
                backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1f),
            )
            ColorPreviewTile(
                title = stringResource(R.string.settings_palette_secondary),
                backgroundColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.weight(1f),
            )
            ColorPreviewTile(
                title = stringResource(R.string.settings_palette_tertiary),
                backgroundColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.weight(1f),
            )
        }

        // Fila de tonos de superficie
        SurfaceTonesRow()
    }
}

@Composable
private fun ColorPreviewTile(
    title: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Aa",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun SurfaceTonesRow(modifier: Modifier = Modifier) {
    val tones = listOf(
        R.string.settings_palette_tone_lowest to MaterialTheme.colorScheme.surfaceContainerLowest,
        R.string.settings_palette_tone_surface to MaterialTheme.colorScheme.surface,
        R.string.settings_palette_tone_low to MaterialTheme.colorScheme.surfaceContainerLow,
        R.string.settings_palette_tone_base to MaterialTheme.colorScheme.surfaceContainer,
        R.string.settings_palette_tone_high to MaterialTheme.colorScheme.surfaceContainerHigh,
        R.string.settings_palette_tone_highest to MaterialTheme.colorScheme.surfaceContainerHighest,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        tones.forEach { (labelRes, color) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(labelRes),
                    fontSize = 8.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
