package com.example.uade.rememberapp.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.AppLockType
import com.example.uade.rememberapp.ui.components.AppDialogDefaults
import com.example.uade.rememberapp.ui.components.DialogFilledConfirmButton

/**
 * Diálogo modal para configurar o cambiar el PIN o contraseña de bloqueo de la app.
 */
@Composable
fun SetupPasswordDialog(
    lockType: AppLockType,
    hasCurrentPassword: Boolean,
    onVerifyCurrent: (String) -> Boolean,
    onSaveNewPassword: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var currentInput by remember { mutableStateOf("") }
    var newInput by remember { mutableStateOf("") }
    var confirmInput by remember { mutableStateOf("") }

    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isPin = lockType == AppLockType.Pin
    val keyboardType = if (isPin) KeyboardType.NumberPassword else KeyboardType.Password

    val titleRes = when {
        isPin && hasCurrentPassword -> R.string.security_dialog_change_pin_title
        isPin -> R.string.security_dialog_setup_pin_title
        hasCurrentPassword -> R.string.security_dialog_change_password_title
        else -> R.string.security_dialog_setup_password_title
    }

    val mismatchError = stringResource(R.string.security_dialog_error_mismatch)
    val invalidCurrentError = stringResource(R.string.security_dialog_error_invalid_current)
    val tooShortPinError = stringResource(R.string.security_dialog_error_too_short_pin)
    val tooShortPwdError = stringResource(R.string.security_dialog_error_too_short_pwd)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppDialogDefaults.shape,
        containerColor = AppDialogDefaults.containerColor,
        title = {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (hasCurrentPassword) {
                    OutlinedTextField(
                        value = currentInput,
                        onValueChange = {
                            currentInput = it
                            errorMessage = null
                        },
                        label = {
                            Text(
                                stringResource(
                                    if (isPin) R.string.security_dialog_current_pin
                                    else R.string.security_dialog_current_password,
                                ),
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = newInput,
                    onValueChange = {
                        if (isPin) {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                newInput = it
                                errorMessage = null
                            }
                        } else {
                            newInput = it
                            errorMessage = null
                        }
                    },
                    label = {
                        Text(
                            stringResource(
                                if (isPin) R.string.security_dialog_new_pin
                                else R.string.security_dialog_new_password,
                            ),
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                painter = painterResource(
                                    if (showPassword) R.drawable.ic_visibility_off else R.drawable.ic_visibility,
                                ),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = confirmInput,
                    onValueChange = {
                        if (isPin) {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                confirmInput = it
                                errorMessage = null
                            }
                        } else {
                            confirmInput = it
                            errorMessage = null
                        }
                    },
                    label = {
                        Text(
                            stringResource(
                                if (isPin) R.string.security_dialog_confirm_pin
                                else R.string.security_dialog_confirm_password,
                            ),
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            DialogFilledConfirmButton(
                text = stringResource(R.string.security_dialog_btn_save),
                onClick = {
                    if (hasCurrentPassword && !onVerifyCurrent(currentInput)) {
                        errorMessage = invalidCurrentError
                        return@DialogFilledConfirmButton
                    }
                    if (isPin && (newInput.length < 4 || newInput.length > 6)) {
                        errorMessage = tooShortPinError
                        return@DialogFilledConfirmButton
                    }
                    if (!isPin && newInput.length < 4) {
                        errorMessage = tooShortPwdError
                        return@DialogFilledConfirmButton
                    }
                    if (newInput != confirmInput) {
                        errorMessage = mismatchError
                        return@DialogFilledConfirmButton
                    }

                    onSaveNewPassword(newInput)
                    onDismiss()
                },
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.security_dialog_btn_cancel))
            }
        },
    )
}
