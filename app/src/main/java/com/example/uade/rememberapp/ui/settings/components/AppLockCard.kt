package com.example.uade.rememberapp.ui.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.AppLockType

/**
 * Tarjeta de configuración del bloqueo de la aplicación (PIN o Contraseña).
 */
@Composable
fun AppLockCard(
    isAppLockEnabled: Boolean,
    lockType: AppLockType,
    hasConfiguredPassword: Boolean,
    onAppLockToggled: (Boolean) -> Unit,
    onLockTypeSelected: (AppLockType) -> Unit,
    onSavePassword: (String) -> Unit,
    onVerifyCurrentPassword: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var showSetupDialog by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Fila principal: Ícono de candado + Título + Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_security_lock_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.settings_security_lock_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Switch(
                    checked = isAppLockEnabled,
                    onCheckedChange = { enabled ->
                        onAppLockToggled(enabled)
                        if (enabled && !hasConfiguredPassword) {
                            showSetupDialog = true
                        }
                    },
                )
            }

            // Opciones detalladas cuando el bloqueo está activado
            AnimatedVisibility(
                visible = isAppLockEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    Text(
                        text = stringResource(R.string.settings_security_lock_type_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )

                    // Selector de tipo: PIN vs Contraseña
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        LockTypeOptionItem(
                            icon = R.drawable.ic_password,
                            title = stringResource(R.string.settings_security_lock_type_pin),
                            subtitle = stringResource(R.string.settings_security_lock_type_pin_desc),
                            isSelected = lockType == AppLockType.Pin,
                            onClick = { onLockTypeSelected(AppLockType.Pin) },
                            modifier = Modifier.weight(1f),
                        )

                        LockTypeOptionItem(
                            icon = R.drawable.ic_lock,
                            title = stringResource(R.string.settings_security_lock_type_password),
                            subtitle = stringResource(R.string.settings_security_lock_type_password_desc),
                            isSelected = lockType == AppLockType.Password,
                            onClick = { onLockTypeSelected(AppLockType.Password) },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // Estado de configuración y botón de acción
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (hasConfiguredPassword) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (hasConfiguredPassword) R.drawable.ic_check_circle else R.drawable.ic_info,
                                    ),
                                    contentDescription = null,
                                    tint = if (hasConfiguredPassword) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(
                                        if (hasConfiguredPassword) {
                                            R.string.settings_security_status_configured
                                        } else {
                                            R.string.settings_security_status_not_configured
                                        },
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            if (hasConfiguredPassword) {
                                OutlinedButton(
                                    onClick = { showSetupDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text(
                                        text = stringResource(
                                            if (lockType == AppLockType.Pin) {
                                                R.string.settings_security_action_change_pin
                                            } else {
                                                R.string.settings_security_action_change_password
                                            },
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { showSetupDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text(
                                        text = stringResource(
                                            if (lockType == AppLockType.Pin) {
                                                R.string.settings_security_action_setup_pin
                                            } else {
                                                R.string.settings_security_action_setup_password
                                            },
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSetupDialog) {
        SetupPasswordDialog(
            lockType = lockType,
            hasCurrentPassword = hasConfiguredPassword,
            onVerifyCurrent = onVerifyCurrentPassword,
            onSaveNewPassword = onSavePassword,
            onDismiss = { showSetupDialog = false },
        )
    }
}

@Composable
private fun LockTypeOptionItem(
    icon: Int,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        border = if (isSelected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        },
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
