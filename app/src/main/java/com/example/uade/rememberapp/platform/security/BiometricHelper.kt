package com.example.uade.rememberapp.platform.security

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.hardware.fingerprint.FingerprintManagerCompat

/**
 * Estado de disponibilidad de la autenticación biométrica en el dispositivo.
 */
enum class BiometricStatus {
    /** Hardware presente y datos biométricos (huella o rostro) enrolados y listos. */
    Available,

    /** El sensor biométrico está disponible pero no hay huellas registradas en Android. */
    NotEnrolled,

    /** El dispositivo no cuenta con sensor biométrico. */
    NoHardware,

    /** Funcionalidad biométrica no disponible o no soportada. */
    Unsupported,
}

/**
 * Utilidades para detección e interacción con la autenticación biométrica de Android.
 */
object BiometricHelper {

    /**
     * Verifica la disponibilidad del sensor biométrico en el dispositivo actual.
     */
    @Suppress("DEPRECATION")
    fun checkBiometricStatus(context: Context): BiometricStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val biometricManager = context.getSystemService(BiometricManager::class.java)
                ?: return BiometricStatus.NoHardware
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            when (biometricManager.canAuthenticate(authenticators)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NotEnrolled
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NoHardware
                else -> BiometricStatus.Unsupported
            }
        } else {
            val fingerprintCompat = FingerprintManagerCompat.from(context)
            if (!fingerprintCompat.isHardwareDetected) {
                BiometricStatus.NoHardware
            } else if (!fingerprintCompat.hasEnrolledFingerprints()) {
                BiometricStatus.NotEnrolled
            } else {
                BiometricStatus.Available
            }
        }
    }

    /**
     * Lanza el diálogo nativo [BiometricPrompt] para autenticar al usuario.
     */
    fun authenticate(
        activity: Activity,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit = { _, _ -> },
        onFailed: () -> Unit = {},
    ): CancellationSignal? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onError(-1, "Biometría nativa requiere Android 9 o superior")
            return null
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val cancellationSignal = CancellationSignal()

        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButton(negativeButtonText, executor) { _, _ ->
                onError(BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED, negativeButtonText)
            }
            .build()

        prompt.authenticate(
            cancellationSignal,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString ?: "")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            },
        )

        return cancellationSignal
    }

    /**
     * Abre los ajustes del sistema operativo para registrar huella o rostro.
     */
    fun openSecuritySettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK,
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }

        try {
            context.startActivity(intent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
                )
            } catch (_: Exception) {
                // Si no se puede abrir ajustes, se ignora de forma segura
            }
        }
    }
}
