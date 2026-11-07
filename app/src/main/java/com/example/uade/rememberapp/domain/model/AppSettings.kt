package com.example.uade.rememberapp.domain.model

/**
 * Modo de tema visual de la aplicación.
 */
enum class ThemeMode {
    /** Sigue la configuración de tema claro u oscuro del sistema Android. */
    System,

    /** Siempre tema claro. */
    Light,

    /** Siempre tema oscuro. */
    Dark,
}

/**
 * Paleta de colores principal de la app.
 */
enum class AppPalette {
    /** Dinámico (Material You): toma los colores del fondo de pantalla en Android 12+. */
    Dynamic,

    /** Azul Hey!: el celeste y azul característico del diseño original. */
    HeyBlue,

    /** Verde Esmeralda: tonos frescos de verde y menta. */
    EmeraldGreen,

    /** Naranja Coral: tonos cálidos de coral, naranja y durazno. */
    CoralOrange,

    /** Oliva & Naturaleza: tonos orgánicos de verde musgo y tierra. */
    OliveEarth,

    /** Ámbar & Miel: tonos dorados, cálidos y luminosos. */
    AmberGold,

    /** Violeta & Lavanda: tonos creativos de violeta y púrpura suave. */
    PurpleLavender,

    /** Rosa & Frambuesa: tonos vibrantes y expresivos de rosa. */
    RosePink,
}

/**
 * Escala de tamaño de texto de la interfaz para accesibilidad.
 */
enum class FontScale(val factor: Float) {
    Small(0.85f),
    Default(1.0f),
    Medium(1.15f),
    Large(1.30f),
}

/**
 * Familia tipográfica pensada para accesibilidad y legibilidad.
 */
enum class AppFontFamily {
    /** Fuente del sistema (Roboto), balanceada y predeterminada. */
    System,

    /**
     * Atkinson Hyperlegible: diseñada por el Braille Institute para maximizar
     * la distinción de caracteres y la legibilidad en personas con baja visión y dislexia.
     */
    AtkinsonHyperlegible,

    /** Sans Serif geométrica y moderna con formas abiertas. */
    SansSerif,

    /** Serif clásica con remates para lectura prolongada. */
    Serif,

    /**
     * Monospace: ancho de caracteres constante, ideal para distinguir números
     * y caracteres similares sin ambigüedad.
     */
    Monospace,
}

/**
 * Tipo de sonido de alarma para las notificaciones de recordatorios.
 */
enum class AlarmSoundType {
    /** Un único tono breve de notificación. */
    ShortChime,

    /** Alarma continua e insistente durante un minuto entero. */
    OneMinuteAlarm,
}

/**
 * Preferencias detalladas para la gestión de notificaciones y alertas.
 */
data class NotificationSettings(
    val timeAlertsEnabled: Boolean = true,
    val placeAlertsEnabled: Boolean = true,
    val highPriorityOnly: Boolean = false,
    val reliableRemindersEnabled: Boolean = true,
    val notifyOverdueReminders: Boolean = true,
    val alarmSoundType: AlarmSoundType = AlarmSoundType.ShortChime,
    val vibrationEnabled: Boolean = true,
)

/**
 * Método o tipo de código de bloqueo para acceder a la aplicación.
 */
enum class AppLockType {
    /** Bloqueo mediante código numérico (PIN). */
    Pin,

    /** Bloqueo mediante contraseña alfanumérica. */
    Password,
}

/**
 * Intervalo de inactividad tras salir o minimizar la app antes de volver a bloquearla.
 */
enum class LockTimeout(val seconds: Int) {
    /** Bloqueo inmediato al salir o poner en segundo plano. */
    Immediately(0),

    /** Bloqueo tras 1 minuto en segundo plano. */
    AfterOneMinute(60),

    /** Bloqueo tras 5 minutos en segundo plano. */
    AfterFiveMinutes(300),
}

/**
 * Preferencias detalladas de seguridad, bloqueo y biometría.
 */
data class SecuritySettings(
    val isAppLockEnabled: Boolean = false,
    val lockType: AppLockType = AppLockType.Pin,
    val biometricUnlockEnabled: Boolean = false,
    val lockTimeout: LockTimeout = LockTimeout.Immediately,
    val passwordHash: String = "",
) {
    val hasConfiguredPassword: Boolean get() = passwordHash.isNotBlank()
}

/**
 * Preferencias generales de la app.
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val usePureBlack: Boolean = false,
    val palette: AppPalette = AppPalette.HeyBlue,
    val fontScale: FontScale = FontScale.Default,
    val fontFamily: AppFontFamily = AppFontFamily.System,
    val useGradientBackground: Boolean = true,
    val notifications: NotificationSettings = NotificationSettings(),
    val security: SecuritySettings = SecuritySettings(),
)
