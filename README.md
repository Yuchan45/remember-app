# Hey! (`Remember App`)

**Hey!** es una aplicación Android nativa de gestión de recordatorios, notas, listas de verificación (*checklists*), mensajes programados y captura de voz asistida por IA, desarrollada en **Kotlin** y **Jetpack Compose** como proyecto académico para la materia **Desarrollo de Aplicaciones I** (*Universidad Argentina de la Empresa — UADE*).

La aplicación permite capturar un pendiente rápidamente (por texto, lista, foto o nota de voz) y decidir **cuándo o dónde debe volver a aparecer**: sin aviso, en una fecha/hora puntual (con repetición opcional), o al llegar/salir de un lugar guardado mediante geocercas en segundo plano.

---

## Tabla de contenidos

1. [Instrucciones de ejecución](#instrucciones-de-ejecución)
2. [Arquitectura final](#arquitectura-final)
3. [Tecnologías utilizadas](#tecnologías-utilizadas)
4. [Funcionalidades implementadas](#funcionalidades-implementadas)
5. [Requisitos funcionales alcanzados](#requisitos-funcionales-alcanzados)
6. [Decisiones relevantes](#decisiones-relevantes)
7. [Limitaciones conocidas](#limitaciones-conocidas)

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

---

## Tecnologías utilizadas

| Categoría | Tecnología / Librería | Versión | Uso en el proyecto |
| :--- | :--- | :--- | :--- |
| **Lenguaje** | Kotlin | `2.2.10` | Lenguaje principal de toda la aplicación |
| **Build System** | Android Gradle Plugin (AGP) + KSP | `9.3.3` / `2.3.12` | Compilación y generación de código (Room) |
| **UI & Diseño** | Jetpack Compose + Material 3 | BOM `2026.02.01` | Interfaces declarativas, tema dinámico y animaciones |
| **Navegación** | Navigation Compose + Compose Foundation | `2.9.5` / `1.12.1` | Grafo de rutas y `HorizontalPager` de pestañas |
| **Ciclo de vida** | Lifecycle ViewModel & Runtime Compose | `2.11.0` | `ViewModel`, `StateFlow`, `collectAsStateWithLifecycle` |
| **Imágenes** | Coil Compose | `3.3.0` | Carga eficiente de fotos locales y portadas |
| **Base de datos** | Room (`room-runtime`, `room-ktx`, `room-compiler`) | `2.8.5` | Persistencia SQLite reactiva con `Flow` y transacciones |
| **Preferencias** | Android `SharedPreferences` | SDK Nativo | Ajustes de tema, accesibilidad, notificaciones y hash de seguridad |
| **Red & JSON** | Retrofit + OkHttp + Gson | `2.11.0` / `4.12.0` / `2.11.0` | Cliente REST para Groq API y serialización de backups JSON |
| **IA (Voz y NLP)** | Groq API (`whisper-large-v3` + `openai/gpt-oss-120b`) | REST v1 | Transcripción de audio en español y extracción estructurada en JSON |
| **Tareas en 2.º plano** | WorkManager (`work-runtime-ktx`) | `2.10.0` | Procesamiento offline-first de audio y purga diaria de historial |
| **Alarmas** | Android `AlarmManager` + `BroadcastReceiver` | SDK Nativo | Alarmas exactas (`setAlarmClock`), acciones en notificación y `BootReceiver` |
| **Ubicación y Mapas** | Google Play Services Location + OSMDroid + `Geocoder` | `21.3.0` / `6.1.20` | Geofencing en segundo plano, búsqueda de direcciones y mapas OpenStreetMap |
| **Audio y Seguridad** | `MediaRecorder` / `MediaPlayer` + `BiometricPrompt` | SDK Nativo | Grabación/reproducción `.m4a` y bloqueo por PIN/contraseña/biometría |
| **Tipografía y Recursos** | `Atkinson Hyperlegible` + Vector Drawables | Local (`res/font`, `res/drawable`) | Tipografía accesible de alta legibilidad e íconos Material Symbols |
| **Testing** | JUnit + AndroidX Test + Espresso + Compose UI Test | `4.13.2` / `1.3.0` / `3.7.0` | Pruebas unitarias de dominio, mappers, repositorios y ViewModels |

---

## Funcionalidades implementadas

### 1. Captura y edición de recordatorios
- **Captura rápida (`QuickCaptureSheet`)**: Creación ágil desde la barra inferior de la Home con acceso directo a atajos de hora/repetición, lugares, importancia, etiquetas, fotos y salto al editor de lista.
- **Editor completo (`ReminderDetailScreen`)**: Título, descripción, listas de verificación (*checklist*) con ítems, notas por ítem y reordenamiento por arrastre (*drag & drop*), múltiples alarmas, múltiples alertas por lugar, etiquetas, importancia (`Low`, `Default`, `High`, `Critical`), acción rápida de notificación y hasta **15 fotos** por recordatorio con **recorte 16:9 de portada** (`ImageCropDialog`).
- **Gestión de etiquetas (`TagPickerPanel`)**: Creación, edición, eliminación y selección de etiquetas con selector de color por matiz (`HueSlider`).

### 2. Pantalla principal (`RemindersListScreen`)
- **Agrupación reactiva por urgencia temporal**: Secciones colapsables (`Vencidos`, `Hoy`, `Mañana`, `Próximos`, `Sin fecha`).
- **Modos de visualización**: Cambio entre lista vertical y grilla de 2 columnas.
- **Búsqueda instantánea**: Filtrado por título, descripción, ítems de checklist, nombres de etiquetas y nombres de lugares asociados.
- **Interacción directa en tarjetas**: Deslizar para *Completar*, *Archivar* o *Enviar a papelera* con barra de *Deshacer* (`Snackbar`); tildado de ítems de checklist directo desde la tarjeta, botón *"Marcar todo"* con confirmación, y diálogo preventivo al intentar completar una nota con ítems pendientes.

### 3. Captura por voz, IA y revisión (`VoiceCaptureSheet` & `ReviewProposedRemindersScreen`)
- **Grabación integrada**: Captura de audio `.m4a` (AAC) con pausa/reanudación, temporizador y visualizador de amplitud en tiempo real (`VoiceWaveform`).
- **Dos modos de captura**:
  - **Con IA activada**: Transcripción con Whisper (`whisper-large-v3`) y estructuración con `openai/gpt-oss-120b` para detectar títulos, descripciones, fechas/horas relativas en español rioplatense, prioridades, lugares sugeridos, checklists o múltiples recordatorios independientes en un mismo dictado.
  - **Con IA desactivada**: Permite pasar el audio directamente a la captura rápida para configurarlo manualmente.
- **Flujo *Offline-First***: Si no hay conexión o falla la red al enviar a Groq, el audio se guarda en Room con estado `Pending` y se encola en `WorkManager` (`AudioProcessingWorker`) para procesarse automáticamente al reconectar y notificar el resultado.
- **Pantalla de revisión de propuestas IA**: Permite seleccionar qué propuestas crear, alternar entre vista **Checklist** y **Separados**, unir propuestas (`MergeProposedRemindersUseCase`), desunir/dividir (`SplitProposedReminderUseCase`), editar cada propuesta en modo borrador con `ReminderDetailViewModel`, y reproducir audio + leer transcripción en el detalle (`VoiceNoteSection`).

### 4. Alarmas, repeticiones y notificaciones
- **Múltiples alarmas por recordatorio** con selector rápido (`TimeShortcut`), calendario mensual (`MonthCalendar`), selector de reloj (`ClockTimePickerDialog`) y repetición (`None`, `Daily`, `Weekdays`, `Weekly`).
- **Avance automático y recuperación**: Al sonar una alarma repetitiva, `AdvanceRepeatingAlarmUseCase` calcula y programa la siguiente ocurrencia respetando zona horaria; `BootReceiver` y el retorno a primer plano (`MainActivity`) reprograman alarmas y geocercas activas.
- **Notificaciones enriquecidas (`NotificationHelper`)**:
  - Canales separados por nivel de importancia (desde silenciosa/mínima hasta `Critical` con sonido de alarma e insistencia `FLAG_INSISTENT`).
  - Miniatura e imagen expandida (`BigPictureStyle`) usando la portada recortada.
  - Botones de acción directa en la notificación: **Hecho / Marcar todo y finalizar** y **Posponer 15 min** (procesados en segundo plano por `ReminderActionReceiver`).

### 5. Mensajes programados y acciones rápidas
- **Pantalla dedicada (`ScheduledMessageScreen`)**: Creación y edición de mensajes programados por **WhatsApp**, **SMS**, **Correo electrónico** o **Instagram**, con selector de contactos del teléfono (`ContactPickerHelper`), cuerpo del mensaje, asunto (en correo), portada/fotos, alarmas y alertas por lugar.
- **Acción rápida en cualquier recordatorio (`NotificationActionSheet`)**: Permite asociar una acción de llamada telefónica, mensaje o correo que aparece como botón ejecutable en la notificación (`MessageLauncherHelper`).

### 6. Lugares y geolocalización (`PlacesListScreen` & `PlaceEditSheet`)
- Alta, edición y eliminación de lugares con categoría e ícono (`Home`, `Work`, `Study`, `Parking`, `Other`).
- Búsqueda de direcciones mediante `Geocoder`, obtención de ubicación actual (`LocationManager`) y previsualización interactiva en mapa **OpenStreetMap** (`PlaceMapPreview`) con círculo visual del radio de detección configurable.
- Alertas de **Llegada** (`Arrive`) y **Salida** (`Leave`) registradas como geocercas en Google Play Services (`GeofenceRegistrarImpl`) y atendidas en segundo plano por `GeofenceReceiver`.

### 7. Historial y purga automática (`HistoryScreen`)
- Tres pestañas: **Hechos**, **Archivados** y **Eliminados**.
- Restauración hacia la Home con opción *Deshacer*, eliminación permanente individual y vaciado masivo por pestaña con doble confirmación.
- Limpieza física de archivos de fotos, portadas y grabaciones de audio sin referencias restantes (`DeleteRemindersForeverUseCase`).
- Purga periódica en segundo plano (`PurgeRemindersWorker`, cada 24 h): elimina definitivamente los recordatorios **Hechos** tras **7 días** y los **Eliminados** tras **15 días**; los **Archivados** se conservan indefinidamente.

### 8. Ajustes, seguridad, accesibilidad y respaldos (`SettingsScreen`)
- **Apariencia y accesibilidad**: Tema (*Sistema*, *Claro*, *Oscuro*, *Negro puro AMOLED*), 8 paletas de color (incluyendo *Material You* dinámico), fondo degradado activable, 5 familias tipográficas (incluyendo `Atkinson Hyperlegible`) y 4 escalas de tamaño de fuente (`0.85x` a `1.30x`).
- **Notificaciones**: Interruptores para alertas por hora, por lugar, solo alta importancia, tipo de sonido (*Tono breve* / *Alarma de 1 minuto*), vibración y tarjetas de confiabilidad/recordatorios vencidos.
- **Seguridad (`AppLockScreen`)**: Bloqueo por PIN o contraseña (hash SHA-256 con salt), autenticación biométrica (`BiometricPrompt`) y tiempo de bloqueo tras salir de la app (*Inmediato*, *1 min*, *5 min*).
- **Datos y almacenamiento (`DataManagementRepositoryImpl`)**: Creación y restauración de copia de seguridad local en JSON, exportación mediante `FileProvider` y selector de importación JSON, cálculo y limpieza de caché, y restablecimiento total de datos.

---

## Requisitos funcionales alcanzados

| Área | Requisito funcional alcanzado | Estado |
| :--- | :--- | :--- |
| **Gestión de pendientes** | Crear, consultar, editar, buscar, completar, archivar, restaurar y eliminar permanentemente recordatorios (notas, checklists y mensajes programados). | Alcanzado |
| **Checklists** | Gestionar ítems individuales con notas, estado de completitud, reordenamiento, marcado masivo y validación de ítems pendientes al finalizar. | Alcanzado |
| **Multimedia** | Adjuntar múltiples imágenes locales (hasta 15), corregir orientación EXIF, recortar portada 16:9 y eliminar archivos huérfanos al borrar definitivamente. | Alcanzado |
| **Etiquetas** | Crear, editar, eliminar y asociar etiquetas personalizadas con color para organizar recordatorios. | Alcanzado |
| **Disparadores temporales** | Configurar múltiples alarmas por recordatorio, atajos, fecha/hora libre, repeticiones periódicas (*Diario*, *Lunes a viernes*, *Semanal*) y reprogramación tras reinicio. | Alcanzado |
| **Geolocalización** | Registrar lugares favoritos sobre mapa, buscar direcciones, configurar radio y activar alertas por llegada o salida mediante geocercas. | Alcanzado |
| **Notificaciones interactivas** | Emitir notificaciones diferenciadas por importancia con portada y acciones directas (*Hecho*, *Posponer 15 min*, *Llamar / Enviar mensaje / Correo*). | Alcanzado |
| **Captura por voz con IA** | Grabar audio, transcribir y extraer uno o varios recordatorios/checklists con revisión humana previa, además de cola *Offline-First* con `WorkManager`. | Alcanzado |
| **Historial y retención** | Separar recordatorios Hechos, Archivados y Eliminados con purga automática periódica (7 y 15 días respectivamente). | Alcanzado |
| **Privacidad y respaldos** | Proteger el acceso con PIN/contraseña + biometría y exportar/importar/restaurar copias de seguridad JSON locales. | Alcanzado |
| **Accesibilidad y personalización** | Configurar tema claro/oscuro/AMOLED, paletas de color, fuente `Atkinson Hyperlegible` y escala tipográfica reactiva. | Alcanzado |
| **Degradación de permisos** | Solicitar permisos esenciales al inicio y permisos específicos en contexto, manteniendo la app operativa con avisos claros ante denegación (`RNF04` / `RNF07`). | Alcanzado |

---

## Decisiones relevantes

1. **Clean Architecture + DI manual en `AppContainer`**: Se optó por inyección manual perezosa (`by lazy`) en lugar de frameworks como Hilt o Koin para mantener transparencia total del grafo de dependencias, tiempos de compilación bajos y facilidad para instanciar ViewModels y casos de uso en pruebas unitarias sin dependencias externas.
2. **Single-Activity + `HorizontalPager` sin swipe entre pestañas**: `MainTabs` desactiva el desplazamiento horizontal con el dedo en el `HorizontalPager` (`userScrollEnabled = false`) para evitar conflictos táctiles con el gesto horizontal de *swipe-to-reveal* en las tarjetas de recordatorios y con las interacciones de mapa.
3. **Resistencia a pérdida de datos en Room**: Se prohibió `fallbackToDestructiveMigration`, manteniendo migraciones explícitas versión a versión (`3→4` hasta `12→13`) acompañadas de `DatabaseBackup` antes de abrir la base tras una actualización.
4. **Copiado local de fotos en lugar de persistir URIs**: Android revoca permisos temporales sobre `content://` URIs tras reiniciar el dispositivo. `PhotoRepositoryImpl` copia, reescala (máx. 2048 px), orienta según EXIF y guarda las imágenes en el almacenamiento privado de la app (`filesDir/photos`), generando además una portada recortada (máx. 1280 px) apta para `NotificationCompat.BigPictureStyle`.
5. **Estrategia de `AlarmManager` con URIs únicas y fallback**: Cada alarma usa un `PendingIntent` identificado por una URI única (`reminder-alarm://<id>/<millis>`) para soportar múltiples alarmas por recordatorio sin sobrescribirse; además se usa `setAlarmClock` cuando hay permiso de alarmas exactas y `setAndAllowWhileIdle` como respaldo cuando no está concedido (`RNF04`).
6. **Canales de notificación por `Importance`**: Desde Android 8, el sonido, la vibración y el comportamiento *heads-up* dependen del `NotificationChannel`. Se crearon canales dedicados (`reminders_low`, `reminders_default`, `reminders_high`, `reminders_critical`, `ai_channel`, `scheduled_messages_channel_v3`) para respetar la prioridad elegida por el usuario.
7. **IA híbrida con revisión previa y cola `WorkManager`**: En lugar de guardar directamente lo inferido por el LLM, el flujo de voz pasa por `ReviewProposedRemindersScreen` para evitar errores de segmentación o fechas mal interpretadas; ante pérdida de conectividad, `AudioProcessingWorker` asegura que ninguna grabación se pierda.
8. **OpenStreetMap (`OSMDroid`) + `Geocoder` nativo**: Se eligió `OSMDroid` con filtro de matriz de color oscuro en lugar de Google Maps SDK para evitar la dependencia de API Keys comerciales y facturación de mapas, manteniendo coherencia visual con la estética nocturna de la app.

---

## Limitaciones conocidas

- **Etiquetas fijas en atajos de hora (`TimeShortcut`)**: La lógica de resolución (`resolveReminderTime`) avanza correctamente *"Hoy 21:00"* al día siguiente o *"Sáb 10:00"* a la semana siguiente si la hora ya pasó, pero los textos mostrados en los chips de selección son cadenas estáticas en `strings.xml`.
- **Preferencias de notificaciones sin consumo activo en *receivers***: Los interruptores `reliableRemindersEnabled` y `notifyOverdueReminders` se persisten en `SettingsRepositoryImpl` (y `ReliableRemindersCard` ofrece accesos directos a los ajustes de batería y alarmas del sistema), pero actualmente `AlarmReceiver`, `GeofenceReceiver` y `NotificationHelper` no consultan dichas banderas para reintentos adicionales o notificaciones periódicas de tareas vencidas.
- **Comprobación de actualizaciones simulada**: En *Ajustes → Acerca de Hey!*, `SettingsViewModel.onCheckForUpdates()` simula una verificación con un retardo (`delay(1200)`) y devuelve siempre `UpdateCheckStatus.UpToDate`, sin consultar un servicio remoto.
- **Respaldo JSON sin archivos multimedia**: La exportación/importación de copias de seguridad (`DataManagementRepositoryImpl`) incluye recordatorios, checklists, alarmas, lugares, alertas por lugar, etiquetas, transcripciones y acciones rápidas, pero **no empaqueta los archivos físicos de fotos (`filesDir/photos`) ni de audio (`filesDir/audios`)**.
- **Dependencia de red y clave de Groq para transcripción de voz**: La transcripción con `whisper-large-v3` requiere conexión a Internet y una `groq.api.key` válida. Sin clave o con clave inválida, la grabación se guarda sin transcribir; el analizador heurístico local (`createLocalHeuristicResult`) actúa únicamente como respaldo textual en `analyzeText`, no sobre audio sin transcribir.
- **Restricciones propias del sistema Android**:
  - Desde Android 8, la vibración y sonido de un `NotificationChannel` quedan fijados al crearse por primera vez; modificar el ajuste de vibración posteriormente no altera canales ya registrados en el sistema salvo reinstalación o ajuste manual del usuario en Android.
  - En mensajes programados / acciones rápidas, el canal **Instagram** abre el perfil web (`https://instagram.com/<usuario>`) porque no existe un *Intent* público estándar para prellenar un mensaje directo.
  - El *geofencing* requiere Google Play Services, ubicación del dispositivo encendida y el permiso `ACCESS_BACKGROUND_LOCATION` (*"Permitir todo el tiempo"*); la búsqueda de direcciones depende del servicio `Geocoder` provisto por el sistema.
- **Integridad referencial de `PlaceAlertEntity` y limpieza por código**: A nivel SQLite, `reminder_places` no declara una `ForeignKey` hacia `places`; la eliminación consistente de alertas asociadas al borrar un lugar se resuelve por aplicación en `PlaceRepositoryImpl.delete`.
- **Orientación de pantalla y tests instrumentados**: `MainActivity` está bloqueada en orientación vertical (`android:screenOrientation="portrait"`). La cobertura automatizada actual se concentra en tests unitarios JVM (`app/src/test`), mientras que `app/src/androidTest` contiene únicamente la plantilla base `ExampleInstrumentedTest.kt`.
