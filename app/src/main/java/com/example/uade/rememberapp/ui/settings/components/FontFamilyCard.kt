package com.example.uade.rememberapp.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.example.uade.rememberapp.domain.model.AppFontFamily
import com.example.uade.rememberapp.ui.theme.resolveFontFamily

/**
 * Card de selección de tipo de fuente con foco en accesibilidad y legibilidad visual.
 */
@Composable
fun FontFamilyCard(
    selectedFontFamily: AppFontFamily,
    onFontFamilySelected: (AppFontFamily) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fontOptions = listOf(
        FontOption(
            family = AppFontFamily.AtkinsonHyperlegible,
            titleRes = R.string.settings_font_family_atkinson,
            descRes = R.string.settings_font_family_atkinson_desc,
            isAccessibilitySpecial = true,
        ),
        FontOption(
            family = AppFontFamily.System,
            titleRes = R.string.settings_font_family_system,
            descRes = R.string.settings_font_family_system_desc,
        ),
        FontOption(
            family = AppFontFamily.SansSerif,
            titleRes = R.string.settings_font_family_sans_serif,
            descRes = R.string.settings_font_family_sans_serif_desc,
        ),
        FontOption(
            family = AppFontFamily.Serif,
            titleRes = R.string.settings_font_family_serif,
            descRes = R.string.settings_font_family_serif_desc,
        ),
        FontOption(
            family = AppFontFamily.Monospace,
            titleRes = R.string.settings_font_family_monospace,
            descRes = R.string.settings_font_family_monospace_desc,
        ),
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.settings_font_family_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_font_family_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                fontOptions.forEach { option ->
                    FontFamilyRowItem(
                        option = option,
                        isSelected = option.family == selectedFontFamily,
                        onClick = { onFontFamilySelected(option.family) },
                    )
                }
            }
        }
    }
}

private data class FontOption(
    val family: AppFontFamily,
    val titleRes: Int,
    val descRes: Int,
    val isAccessibilitySpecial: Boolean = false,
)

@Composable
private fun FontFamilyRowItem(
    option: FontOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolvedFamily = resolveFontFamily(option.family)
    val containerBg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(containerBg)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Círculo con muestra "Aa" en la fuente exacta
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Aa",
                fontFamily = resolvedFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Título y descripción
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(option.titleRes),
                    fontFamily = resolvedFamily,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (option.isAccessibilitySpecial) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "Braille Inst.",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Text(
                text = stringResource(option.descRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Indicador de selección
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}
