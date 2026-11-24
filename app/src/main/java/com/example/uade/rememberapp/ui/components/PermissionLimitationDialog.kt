package com.example.uade.rememberapp.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.permissions.LimitedCapability
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Diálogo que informa al usuario que su recordatorio o lugar se guardó igualmente, qué
 * funcionalidad queda limitada por falta de permisos y cómo habilitarla desde Ajustes del
 * sistema (RNF07 / RNF04).
 */
@Composable
fun PermissionLimitationDialog(
    capabilities: Set<LimitedCapability>,
    isPlaceOnly: Boolean,
    onOpenSettings: (LimitedCapability) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryCapability = capabilities.firstOrNull() ?: return
    val titleRes = if (isPlaceOnly) {
        R.string.permission_limitation_dialog_title_place
    } else {
        R.string.permission_limitation_dialog_title_reminder
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = AppDialogDefaults.shape,
        containerColor = AppDialogDefaults.containerColor,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_priority_high),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                capabilities.forEach { capability ->
                    LimitationItem(
                        icon = capability.iconRes(),
                        titleRes = capability.titleRes(),
                        descriptionRes = capability.descriptionRes(),
                    )
                }
            }
        },
        confirmButton = {
            DialogFilledConfirmButton(
                text = stringResource(R.string.permission_limitation_btn_settings),
                onClick = { onOpenSettings(primaryCapability) },
            )
        },
        dismissButton = {
            DialogDismissButton(
                text = stringResource(R.string.permission_limitation_btn_ok),
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun LimitationItem(
    @DrawableRes icon: Int,
    @StringRes titleRes: Int,
    @StringRes descriptionRes: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@DrawableRes
private fun LimitedCapability.iconRes(): Int = when (this) {
    LimitedCapability.Notifications -> R.drawable.ic_notifications_off
    LimitedCapability.ExactAlarms -> R.drawable.ic_alarm
    LimitedCapability.BackgroundLocation -> R.drawable.ic_location_on
}

@StringRes
private fun LimitedCapability.titleRes(): Int = when (this) {
    LimitedCapability.Notifications -> R.string.permission_limitation_notifications_title
    LimitedCapability.ExactAlarms -> R.string.permission_limitation_exact_alarms_title
    LimitedCapability.BackgroundLocation -> R.string.permission_limitation_location_title
}

@StringRes
private fun LimitedCapability.descriptionRes(): Int = when (this) {
    LimitedCapability.Notifications -> R.string.permission_limitation_notifications_desc
    LimitedCapability.ExactAlarms -> R.string.permission_limitation_exact_alarms_desc
    LimitedCapability.BackgroundLocation -> R.string.permission_limitation_location_desc
}

@Preview
@Composable
private fun PermissionLimitationDialogPreview() {
    RememberAppTheme {
        PermissionLimitationDialog(
            capabilities = setOf(
                LimitedCapability.Notifications,
                LimitedCapability.ExactAlarms,
            ),
            isPlaceOnly = false,
            onOpenSettings = {},
            onDismiss = {},
        )
    }
}
