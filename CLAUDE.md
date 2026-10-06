# Hey! (RememberApp)

App Android de recordatorios para el TP de Desarrollo de Aplicaciones I (UADE). El usuario captura
un pendiente (texto y/o foto) y elige cuándo vuelve: nunca, a una hora, o al llegar a un lugar.

## Stack
- Kotlin + Jetpack Compose + Material3. Una sola Activity (`MainActivity`).
- Navegación: `ui/navigation/AppNavHost.kt` es la raíz (NavHost). Su destino `Routes.MAIN` es `MainTabs`: las 4 pantallas principales como páginas de un `HorizontalPager` (se deslizan de costado) con `AppBottomBar` encima. Las pantallas que se abren encima (detalles) van como rutas en `Routes.kt`.
- ViewModel + `StateFlow` + `collectAsStateWithLifecycle`.
- Persistencia con Room (KSP genera el código). `data/local/AppDatabase.kt` lista las tablas; al cambiar columnas, subir `version` (por ahora borra y recrea la base: `fallbackToDestructiveMigration`).
- DI manual: `AppContainer(context)` crea base, repositorios y casos de uso `by lazy`; los ViewModels con dependencias exponen un `Factory` que los toma de `(application as RememberApp).container`.
- Dependencias en `gradle/libs.versions.toml` (version catalog). Nunca hardcodear versiones en `build.gradle.kts`.
- minSdk 26, targetSdk/compileSdk 37, Java 11.
- Paquete base: `com.example.uade.rememberapp`.

## Arquitectura (clean architecture por capas)
```
app/src/main/java/com/example/uade/rememberapp/
├── domain/        Kotlin puro, sin imports de android.*
│   ├── model/        Reminder, Place, Trigger (sealed)
│   ├── repository/   interfaces (ReminderRepository, PlaceRepository)
│   ├── scheduler/    interfaces (ReminderScheduler, GeofenceRegistrar)
│   └── usecase/      un caso de uso por clase
├── data/          implementaciones
│   ├── local/        Room: dao/, entity/, mapper/
│   ├── repository/   *RepositoryImpl
│   ├── scheduler/    AlarmManager / Geofencing
│   └── storage/      archivos (fotos)
├── platform/      notification/, receiver/ (BroadcastReceivers)
├── ui/
│   ├── components/   componentes genéricos, sin saber de ninguna feature (CircleIconButton, TagChip…)
│   ├── navigation/   NavGraph, rutas y AppBottomBar
│   ├── theme/        Color.kt (paleta del Figma), Theme.kt (siempre oscuro, sin dynamic color)
│   └── <feature>/
│       ├── components/                  compartidos entre pantallas de la feature (ej. ReminderCard)
│       ├── sample/                      datos mock para previews (y para el VM mientras no hay datos)
│       └── <list|edit|detail>/          XScreen.kt + XViewModel.kt + XUiState.kt
│           └── components/              piezas que solo usa esa pantalla (ej. HomeHeader)
├── MainActivity.kt
└── RememberApp.kt  Application; acá vive el AppContainer (DI manual)
```
Dependencias permitidas: `ui → domain`, `data → domain`, `platform → domain`. `domain` no depende de nadie.
La UI nunca importa nada de `data`.

## Convenciones de UI
- Cada pantalla tiene dos composables:
  - `XScreen(viewModel = viewModel())`: **con estado**, solo obtiene el ViewModel y lee `uiState`.
  - `private XContent(uiState, onAlgo: ..., modifier)`: **sin estado**, recibe todo por parámetros y avisa por lambdas. Es el que lleva `@Preview`.
- UiState = `data class XUiState` inmutable con valores por defecto; lo derivado va como `val get()`.
- ViewModel expone `val uiState: StateFlow<XUiState>` (privado `_uiState` + `asStateFlow()`) y funciones `onEvento(...)`.
- Componentes sin estado: el valor entra por parámetro y el cambio sale por callback. `modifier: Modifier = Modifier` siempre es el primer parámetro opcional.
- **Nada de textos hardcodeados**: todo va a `res/values/strings.xml`, agrupado con un comentario por pantalla, con prefijo de feature (`reminders_...`, `places_...`); lo global de navegación usa `nav_...`. En el ViewModel se guarda el `@StringRes Int`, no el String (el ViewModel no tiene Context).
- Colores del tema vía `MaterialTheme.colorScheme`; colores propios en `ui/theme/Color.kt`.
- Íconos: vector drawables de Material Symbols Outlined en `res/drawable/ic_<nombre>.xml` (sin librería de íconos). Se bajan de `google/material-design-icons` (`symbols/web/<nombre>/materialsymbolsoutlined/<nombre>_24px.svg`), con viewport 960 y `<group android:translateY="960">`.
- Si una pantalla tiene muchos eventos, agruparlos en una `data class XActions` con lambdas por defecto (ver `RemindersListActions`).
- Toda preview va envuelta en `RememberAppTheme { }`.

## Estilo de código
- Comentarios KDoc en español, explicando el *por qué* (ver `RemindersListViewModel.kt`).
- Trailing commas en listas de parámetros multilínea.
- `TODO:` en español para lo que queda pendiente.

## Comandos
```bash
./gradlew assembleDebug        # compilar
./gradlew testDebugUnitTest    # tests unitarios
./gradlew lintDebug            # lint
./gradlew connectedDebugAndroidTest  # tests instrumentados (requiere emulador)
```
En PowerShell: `.\gradlew.bat <tarea>`.

## Skills externas
Instaladas con `npx skills add ... -a claude-code --copy` y registradas en `skills-lock.json`
(para restaurarlas: `npx skills experimental_install`; para actualizarlas: `npx skills update -p`):
- Google (`android/skills`): `android-permissions-security`, `android-intent-security`
- Chris Banes (`chrisbanes/skills`): `compose-state-and-effects`, `compose-performance`, `kotlin-concurrency-and-flow`

Son guías genéricas. **Si contradicen este archivo o las skills propias del proyecto, mandan las del proyecto.**
En particular: la app tiene una sola Activity, así que los permisos se piden desde Compose con
`rememberLauncherForActivityResult` y no con Activities nuevas como `RuntimePermissionsActivity`.
Tampoco se agregan dependencias ni plugins sin preguntar.

## Git
- Commits: `tipo(scope): descripción breve`, ej. `feat(list-screen): agrega selector de pestañas`. Usar la skill `commit`.
- No commitear `local.properties`, `build/`, `.gradle/`.
