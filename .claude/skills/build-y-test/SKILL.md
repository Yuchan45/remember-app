---
name: build-y-test
description: Compila la app, corre tests unitarios y lint con Gradle e interpreta los errores. Usar para verificar que un cambio compila, después de modificar código Kotlin, recursos o gradle, o cuando el usuario pida compilar, buildear, testear o correr lint.
---

# Compilar y testear

Correr desde la raíz del proyecto (donde está `gradlew`). En Git Bash: `./gradlew`; en PowerShell: `.\gradlew.bat`.

| Objetivo | Tarea |
|---|---|
| ¿Compila? | `./gradlew assembleDebug` |
| Tests unitarios | `./gradlew testDebugUnitTest` |
| Un test puntual | `./gradlew testDebugUnitTest --tests "*NombreDelTest*"` |
| Lint | `./gradlew lintDebug` (reporte en `app/build/reports/lint-results-debug.html`) |
| Tests instrumentados | `./gradlew connectedDebugAndroidTest` (necesita emulador o dispositivo; preguntar antes) |

## Flujo

1. Después de un cambio, siempre `assembleDebug`. Si se tocaron `domain/` o mappers, también `testDebugUnitTest`.
2. Si falla, leer el **primer** error (`e: file:///...kt:LINEA:COL`); los siguientes suelen ser consecuencia de ese.
3. Corregir y volver a correr hasta que pase. No dar la tarea por terminada con el build roto.
4. Informar el resultado de forma honesta: qué se corrió y si pasó o falló.

## Errores comunes en este proyecto

- **`Unresolved reference: R`** o un string que no existe: falta agregarlo a `res/values/strings.xml` o hay un typo en el nombre.
- **`@Composable invocations can only happen from...`**: se llamó a `stringResource()` desde un ViewModel o desde una lambda no composable. En el ViewModel, guardar el `@StringRes Int`.
- **Errores de KSP / Room** (`[ksp] ...`): query mal escrita o tipo no soportado en la entity. Ver la skill `nueva-entidad-room`.
- **Error de versión de KSP**: la versión de KSP no corresponde a la de Kotlin del catálogo.
- **`Could not resolve ...`** al agregar una dependencia: revisar el nombre en `libs.versions.toml` y el acceso a internet.
- **Warning de lint `HardcodedText` o `SetTextI18n`**: mover el texto a `strings.xml`.

## Notas
- La primera ejecución puede tardar varios minutos (el daemon arranca y descarga dependencias). Usar un timeout largo.
- No correr `./gradlew clean` salvo que haya caché corrupta evidente.
