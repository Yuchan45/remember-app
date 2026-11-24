# Hey! (`Remember App`)

**Hey!** es una aplicación Android nativa de gestión de recordatorios, notas, listas de verificación (*checklists*), mensajes programados y captura de voz asistida por IA, desarrollada en **Kotlin** y **Jetpack Compose** como proyecto académico para la materia **Desarrollo de Aplicaciones I** (*Universidad Argentina de la Empresa — UADE*).

La aplicación permite capturar un pendiente rápidamente (por texto, lista, foto o nota de voz) y decidir **cuándo o dónde debe volver a aparecer**: sin aviso, en una fecha/hora puntual (con repetición opcional), o al llegar/salir de un lugar guardado mediante geocercas en segundo plano.

---

## Tabla de contenidos

1. [Instrucciones de ejecución](#instrucciones-de-ejecución)
2. [Arquitectura final](#arquitectura-final)

---

## Instrucciones de ejecución

### Requisitos previos

- **Android Studio** reciente con soporte para Android Gradle Plugin (`AGP`) **9.3.3** y Kotlin **2.2.10**.
- **Android SDK** con plataforma **API 37** (`compileSdk = 37`, `targetSdk = 37`) y soporte mínimo desde **API 26** (`minSdk = 26`, Android 8.0 Oreo).
- **Java / Gradle JVM**:
  - El código compila con compatibilidad **Java 11** (`sourceCompatibility` / `targetCompatibility = JavaVersion.VERSION_11`).
  - El daemon de Gradle está configurado en `gradle/gradle-daemon-jvm.properties` con `toolchainVersion=25` (resuelto automáticamente mediante `foojay-resolver-convention` si se utiliza el wrapper `./gradlew`).
- **Dispositivo o emulador Android** (API 26+) con **Google Play Services** instalado si se desea probar la detección de geocercas y ubicación.

### 1. Clonar el repositorio

```bash
git clone <URL_DEL_REPOSITORIO>
cd remember-app
```

### 2. Configurar la API Key de Groq (opcional para el núcleo, necesaria para IA por voz)

La transcripción de notas de voz (`whisper-large-v3`) y la estructuración inteligente en recordatorios (`openai/gpt-oss-120b`) consumen la API de **Groq**. La clave se inyecta en tiempo de compilación como `BuildConfig.GROQ_API_KEY` desde `app/build.gradle.kts`.

Existen dos formas de configurarla (no se debe commitear `local.properties`):

- **Opción A — En `local.properties` (recomendada para Android Studio):**
  Agregar o editar en la raíz del proyecto:
  ```properties
  sdk.dir=<RUTA_AL_ANDROID_SDK>
  groq.api.key=gsk_tu_clave_aqui
  ```
- **Opción B — Variable de entorno:**
  ```bash
  # Linux / macOS
  export GROQ_API_KEY="gsk_tu_clave_aqui"
  ```
  ```powershell
  # Windows PowerShell
  $env:GROQ_API_KEY="gsk_tu_clave_aqui"
  ```

> **Nota:** Si no se configura `groq.api.key`, la aplicación compila y funciona normalmente para notas, checklists, fotos, alarmas, geocercas, historial, seguridad y respaldos. Al intentar transcribir una grabación de voz con IA sin clave válida, la nota se guarda localmente con su audio sin transcribir y se informa el motivo al usuario.

### 3. Compilar, ejecutar y testear

#### Desde Android Studio
1. Abrir la carpeta raíz `remember-app` y esperar la sincronización de Gradle.
2. Seleccionar la configuración de ejecución `app` y un emulador o dispositivo físico.
3. Presionar **Run** (`Shift + F10`).

#### Desde línea de comandos

En **Windows (PowerShell)**:
```powershell
.\gradlew.bat assembleDebug               # Compilar APK debug
.\gradlew.bat installDebug                # Instalar en dispositivo/emulador conectado
.\gradlew.bat testDebugUnitTest           # Ejecutar tests unitarios JVM
.\gradlew.bat lintDebug                   # Ejecutar análisis estático Lint
.\gradlew.bat connectedDebugAndroidTest   # Ejecutar tests instrumentados (requiere emulador/dispositivo)
```

En **Linux / macOS**:
```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew connectedDebugAndroidTest
```

### 4. Permisos del dispositivo para pruebas completas

Al iniciar, la app solicita únicamente los permisos esenciales de **Notificaciones** (`POST_NOTIFICATIONS` en Android 13+) y **Alarmas exactas** (`SCHEDULE_EXACT_ALARM` en Android 12+). Los permisos restantes se solicitan de forma contextual al utilizar cada función:
- **Micrófono** (`RECORD_AUDIO`): al grabar una nota de voz.
- **Ubicación precisa y en segundo plano** (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`): al crear lugares o asociar alertas por ubicación (para disparar geocercas con la app cerrada se requiere seleccionar *"Permitir todo el tiempo"* en Android 10+).
- **Contactos** (`READ_CONTACTS`): al seleccionar destinatarios en mensajes programados.
- **Biometría** (`USE_BIOMETRIC`): al habilitar desbloqueo por huella/rostro.

---

## Arquitectura final

La aplicación sigue una arquitectura **Clean Architecture por capas** combinada con **MVVM (Model-View-ViewModel)** y flujo de datos unidireccional (**UDF**) mediante `StateFlow` / `SharedFlow` y `collectAsStateWithLifecycle`.

### Principios estructurales

- **Single-Activity (`MainActivity`)**: Toda la navegación se gestiona en Compose mediante `AppNavHost`. El destino raíz `Routes.MAIN` renderiza `MainTabs`, que aloja las 4 pestañas principales (*Inicio*, *Historial*, *Lugares*, *Ajustes*) dentro de un `HorizontalPager` (con desplazamiento por gesto deshabilitado para no interferir con el *swipe* de las tarjetas) y una barra inferior flotante (`AppBottomBar`) junto al botón de creación (`+`). Las pantallas secundarias (detalle/edición de nota y mensaje programado) se apilan como rutas sobre el `NavHost`.
- **Inyección de dependencias manual (`AppContainer`)**: En `RememberApp` vive una instancia única de `AppContainer`, que construye de forma perezosa (`by lazy`) la base de datos Room, los repositorios, los *schedulers*, el cliente Retrofit de Groq, el grabador de audio y los casos de uso, exponiéndolos hacia los ViewModels mediante `ViewModelProvider.Factory`.
- **Separación por capas y regla de dependencias**:
  - `ui → domain`
  - `data → domain`
  - `platform → domain`
  - La capa `ui` nunca importa implementaciones concretas de `data` (accede a través de casos de uso e interfaces del dominio).

### Organización de paquetes (`com.example.uade.rememberapp`)

```text
app/src/main/java/com/example/uade/rememberapp/
├── RememberApp.kt                 # Application: inicializa AppContainer, canales de notificación, OSMDroid y PurgeRemindersWorker
├── AppContainer.kt                # Contenedor de DI manual (by lazy)
├── MainActivity.kt                # Activity única: tema reactivo, bloqueo de sesión (AppLockScreen), deep links y reprogramación onResume
├── domain/                        # Lógica de negocio y contratos
│   ├── model/                     # Reminder, Alarm, Place, PlaceAlert, Tag, ChecklistItem, NotificationAction, AppSettings, ProposedReminder, backup/*
│   ├── repository/                # Interfaces: ReminderRepository, PlaceRepository, TagRepository, PhotoRepository, SettingsRepository, DataManagementRepository, AiReminderService, AddressSearchService, GroqApiService
│   ├── scheduler/                 # Interfaces: ReminderScheduler, GeofenceRegistrar
│   └── usecase/                   # Casos de uso atómicos (SaveReminder, ObserveReminders, MarkReminderDone, SnoozeReminder, AdvanceRepeatingAlarm, CompleteFromNotification, RescheduleReminders, PurgeExpiredReminders, ProcessVoiceWithAi, Merge/SplitProposed, SavePlace, etc.)
├── data/                          # Implementaciones de persistencia, red y planificación
│   ├── local/                     # AppDatabase (Room v13), Migrations (3→13), DatabaseBackup, dao/, entity/, mapper/
│   ├── remote/                    # GroqReminderServiceImpl y dto/ (Whisper + LLM + fallback heurístico de texto)
│   ├── repository/                # ReminderRepositoryImpl, PlaceRepositoryImpl, TagRepositoryImpl, SettingsRepositoryImpl, DataManagementRepositoryImpl
│   ├── scheduler/                 # AlarmReminderScheduler (AlarmManager) y GeofenceRegistrarImpl (Play Services)
│   └── storage/                   # PhotoRepositoryImpl (copia, rotación EXIF y recorte de portadas en filesDir/photos)
├── platform/                      # Integración directa con el sistema operativo Android
│   ├── audio/                     # AudioRecorder (MediaRecorder .m4a AAC)
│   ├── contacts/                  # ContactPickerHelper
│   ├── location/                  # AndroidAddressSearchService (Geocoder + LocationManager) y LocationPermissionHelper
│   ├── message/                   # MessageLauncherHelper (Intents de llamada, WhatsApp, SMS, Instagram, Email)
│   ├── notification/              # NotificationHelper (canales por importancia, BigPictureStyle, acciones rápidas)
│   ├── permissions/               # PermissionsHelper
│   ├── receiver/                  # AlarmReceiver, GeofenceReceiver, ReminderActionReceiver, BootReceiver
│   ├── security/                  # BiometricHelper (BiometricManager / BiometricPrompt)
│   └── worker/                    # WorkManagerScheduler, AudioProcessingWorker, PurgeRemindersWorker
└── ui/                            # Capa de presentación en Jetpack Compose
    ├── components/                # Componentes reutilizables agnósticos (AppDialog, MonthCalendar, ClockTimePickerDialog, ImageCropDialog, TagChip, etc.)
    ├── navigation/                # AppNavHost, Routes, MainTabs, AppBottomBar
    ├── permissions/               # StartupEssentialPermissionsHandler, TriggerPermissionsCoordinator, PermissionNoticeController
    ├── theme/                     # RememberAppTheme, Color, PaletteScheme, Background, Type, AppLocale
    ├── security/                  # AppLockScreen (PIN, contraseña y biometría)
    ├── reminders/                 # list/, capture/, create/, detail/, edit/, message/, voice/, review/, components/, model/, sample/
    ├── history/                   # HistoryScreen, HistoryViewModel, HistoryUiState
    ├── places/                    # list/, edit/, components/ (PlaceMapPreview con OSMDroid)
    ├── tags/                      # TagPickerPanel, TagPickerViewModel, TagPickerUiState
    └── settings/                  # SettingsScreen, SettingsViewModel, SettingsUiState, components/
```

### Esquema de persistencia local (`AppDatabase`)

Room gestiona 9 entidades relacionadas en `hey.db` (versión actual **13**):
- `reminders` (`ReminderEntity`)
- `tags` (`TagEntity`) y tabla intermedia `reminder_tags` (`ReminderTagCrossRef`)
- `reminder_alarms` (`AlarmEntity`)
- `reminder_places` (`PlaceAlertEntity`)
- `reminder_actions` (`ActionEntity`)
- `reminder_photos` (`PhotoEntity`)
- `reminder_checklist_items` (`ChecklistItemEntity`)
- `places` (`PlaceEntity`)

Todas las evoluciones de esquema desde la versión 3 hasta la 13 cuentan con migraciones explícitas en `Migrations.kt`, sin `fallbackToDestructiveMigration`, y con copia preventiva automática (`DatabaseBackup`) en `databases/backups/` (conservando hasta 5 respaldos) antes de migrar.
