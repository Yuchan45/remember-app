package com.example.uade.rememberapp.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Diálogo educativo previo (Permission Primer) que explica al usuario por qué
 * la aplicación necesita los permisos esenciales (Notificaciones, Ubicación y Micrófono)
 * antes de solicitar las ventanas emergentes del sistema operativo.
 */
@Composable
fun StartupPermissionsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = AppDialogDefaults.shape,
        containerColor = AppDialogDefaults.containerColor,
        title = {
            Text(
                text = stringResource(R.string.startup_permissions_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.startup_permissions_dialog_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                PermissionReasonItem(
                    icon = R.drawable.ic_notifications,
                    title = stringResource(R.string.startup_permissions_item_notifications_title),
                    description = stringResource(R.string.startup_permissions_item_notifications_desc),
                )

                PermissionReasonItem(
                    icon = R.drawable.ic_location_on,
                    title = stringResource(R.string.startup_permissions_item_location_title),
                    description = stringResource(R.string.startup_permissions_item_location_desc),
                )

                PermissionReasonItem(
                    icon = R.drawable.ic_mic,
                    title = stringResource(R.string.startup_permissions_item_audio_title),
                    description = stringResource(R.string.startup_permissions_item_audio_desc),
                )
            }
        },
        confirmButton = {
            DialogFilledConfirmButton(
                text = stringResource(R.string.startup_permissions_btn_continue),
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogDismissButton(
                text = stringResource(R.string.startup_permissions_btn_not_now),
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun PermissionReasonItem(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp,
            )
        }
    }
}

@Preview
@Composable
private fun StartupPermissionsDialogPreview() {
    RememberAppTheme {
        StartupPermissionsDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}
