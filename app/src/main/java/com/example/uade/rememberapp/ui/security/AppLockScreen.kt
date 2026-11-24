package com.example.uade.rememberapp.ui.security

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.AppLockType
import com.example.uade.rememberapp.domain.model.SecuritySettings
import com.example.uade.rememberapp.platform.security.BiometricHelper
import com.example.uade.rememberapp.platform.security.BiometricStatus

/**
 * Pantalla completa de bloqueo de la app que requiere PIN, contraseña o biometría.
 */
@Composable
fun AppLockScreen(
    securitySettings: SecuritySettings,
    onVerifyPassword: (String) -> Boolean,
    onUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var enteredPin by remember { mutableStateOf("") }
    var enteredPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isPin = securitySettings.lockType == AppLockType.Pin
    val wrongCodeText = stringResource(R.string.lock_screen_error_wrong_code)

    val biometricTitle = stringResource(R.string.lock_screen_biometric_prompt_title)
    val biometricSubtitle = stringResource(R.string.lock_screen_biometric_prompt_subtitle)
    val biometricCancel = stringResource(R.string.lock_screen_biometric_prompt_cancel)

    val isBiometricAvailable = remember {
        securitySettings.biometricUnlockEnabled &&
            BiometricHelper.checkBiometricStatus(context) == BiometricStatus.Available
    }

    fun triggerBiometric() {
        if (activity != null && isBiometricAvailable) {
            BiometricHelper.authenticate(
                activity = activity,
                title = biometricTitle,
                subtitle = biometricSubtitle,
                negativeButtonText = biometricCancel,
                onSuccess = { onUnlockSuccess() },
            )
        }
    }

    // Al iniciar, si tiene biometría configurada, lanza el sensor biométrico automáticamente
    LaunchedEffect(isBiometricAvailable) {
        if (isBiometricAvailable) {
            triggerBiometric()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Cabecera: Ícono de candado + Nombre de la app + Subtítulo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lock),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.lock_screen_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Text(
                    text = stringResource(
                        if (isPin) R.string.lock_screen_subtitle_pin else R.string.lock_screen_subtitle_password,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Cuerpo de ingreso: PIN o Contraseña
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isPin) {
                    // Puntos indicadores del PIN
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 24.dp),
                    ) {
                        repeat(4) { index ->
                            val isFilled = index < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                    ),
                            )
                        }
                    }
                } else {
                    // Campo de texto para contraseña alfanumérica
                    OutlinedTextField(
                        value = enteredPassword,
                        onValueChange = {
                            enteredPassword = it
                            errorMessage = null
                        },
                        label = { Text(stringResource(R.string.lock_screen_password_placeholder)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (onVerifyPassword(enteredPassword)) {
                                    onUnlockSuccess()
                                } else {
                                    errorMessage = wrongCodeText
                                }
                            },
                        ),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (onVerifyPassword(enteredPassword)) {
                                onUnlockSuccess()
                            } else {
                                errorMessage = wrongCodeText
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.lock_screen_btn_unlock))
                    }
                }

                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            // Teclado numérico inferior para modo PIN
            if (isPin) {
                Column(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf(
                            if (isBiometricAvailable) "BIO" else "",
                            "0",
                            "DEL",
                        ),
                    )

                    for (row in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            for (item in row) {
                                when (item) {
                                    "BIO" -> {
                                        FilledTonalIconButton(
                                            onClick = { triggerBiometric() },
                                            modifier = Modifier.size(68.dp),
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_fingerprint),
                                                contentDescription = stringResource(R.string.lock_screen_btn_biometric),
                                                modifier = Modifier.size(32.dp),
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                    "DEL" -> {
                                        FilledTonalIconButton(
                                            onClick = {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                    errorMessage = null
                                                }
                                            },
                                            modifier = Modifier.size(68.dp),
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_close),
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    }
                                    "" -> {
                                        Spacer(modifier = Modifier.size(68.dp))
                                    }
                                    else -> {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    if (enteredPin.length < 6) {
                                                        val updated = enteredPin + item
                                                        enteredPin = updated
                                                        errorMessage = null

                                                        if (updated.length >= 4) {
                                                            if (onVerifyPassword(updated)) {
                                                                onUnlockSuccess()
                                                            } else if (updated.length == 6) {
                                                                errorMessage = wrongCodeText
                                                                enteredPin = ""
                                                            }
                                                        }
                                                    }
                                                },
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = item,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (isBiometricAvailable) {
                // Si está en modo contraseña pero tiene biometría, muestra el botón de huella
                FilledTonalIconButton(
                    onClick = { triggerBiometric() },
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fingerprint),
                        contentDescription = stringResource(R.string.lock_screen_btn_biometric),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
