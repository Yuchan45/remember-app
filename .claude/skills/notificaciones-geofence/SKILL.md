---
name: notificaciones-geofence
description: Implementa los avisos de recordatorios (notificaciones, alarmas por hora con AlarmManager, geofences por lugar con Play Services), los BroadcastReceivers y los permisos de runtime. Usar cuando se trabaje con notificaciones, alarmas, ReminderScheduler, GeofenceRegistrar, ubicación, permisos o receivers.
---

# Avisos: notificaciones, alarmas y geofences

## Mapa de piezas

| Interfaz del dominio | Implementación | Dónde |
|---|---|---|
| `ReminderScheduler` | `AlarmReminderScheduler` (AlarmManager) | `data/scheduler/` |
| `GeofenceRegistrar` | `PlayServicesGeofenceRegistrar` (GeofencingClient) | `data/scheduler/` |
| — | `ReminderNotifier` (canal + notificación) | `platform/notification/` |
| — | `ReminderAlarmReceiver`, `GeofenceReceiver`, `BootReceiver` | `platform/receiver/` |

Las implementaciones se crean en el `AppContainer` y se exponen con el tipo de la interfaz del dominio.

## Notificaciones

- Crear el canal una sola vez en `RememberApp.onCreate()` (id constante, por ejemplo `"reminders"`). El nombre y la descripción del canal van en `strings.xml`.
- Android 13+ (API 33) pide el permiso `POST_NOTIFICATIONS` en runtime. Declararlo en el manifest y pedirlo desde la UI con `rememberLauncherForActivityResult(RequestPermission())` en el momento en que el usuario crea su primer recordatorio con aviso, no al abrir la app.
- La notificación abre la app con un `PendingIntent` a `MainActivity` que lleva el `reminderId` como extra. `PendingIntent` siempre con `FLAG_IMMUTABLE`.
- Usar `reminderId.toInt()` como id de la notificación, así un aviso repetido reemplaza al anterior.

## Alarmas por hora (`Trigger.AtTime`)

- `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, ...)` necesita `SCHEDULE_EXACT_ALARM` (API 31+). Verificar `canScheduleExactAlarms()`; si da `false`, usar `setAndAllowWhileIdle` (inexacta) como alternativa, o llevar al usuario a la configuración. **Preguntar al usuario qué prefiere** antes de pedir `USE_EXACT_ALARM`.
- El `PendingIntent` de la alarma usa `requestCode = reminderId.toInt()`, para poder cancelarla con los mismos datos.
- Las alarmas se pierden al reiniciar el teléfono: `BootReceiver` (`RECEIVE_BOOT_COMPLETED`) reprograma todos los recordatorios pendientes con `AtTime` en el futuro.

## Geofences (`Trigger.AtPlace`)

- Requiere agregar `play-services-location` al catálogo de versiones. Preguntar antes de agregar la dependencia.
- Permisos: `ACCESS_FINE_LOCATION` primero y **después, en un paso aparte**, `ACCESS_BACKGROUND_LOCATION` (Android 11+ no deja pedir los dos juntos; el segundo lleva al usuario a Configuración). Explicarle en la UI por qué hace falta.
- Una geofence por **lugar**, no por recordatorio: el id de la geofence es `place.id.toString()` (ver el KDoc de `Place`). `GeofenceRegistrar.ensureRegistered(placeId)` es idempotente.
- Transición `GEOFENCE_TRANSITION_ENTER`, `NEVER_EXPIRE`, radio `place.radiusMeters`.
- `GeofenceReceiver`: obtener el `placeId` → `ReminderRepository.observePendingForPlace(placeId).first()` → notificar. Para el trabajo asíncrono dentro del receiver, usar `goAsync()` con una coroutine, o delegar en WorkManager.
- Al borrar un lugar o cuando ya no le quedan recordatorios pendientes, llamar a `remove(placeId)`.
- Las geofences también se pierden al reiniciar: re-registrarlas en `BootReceiver`.

## Manifest

Declarar cada receiver con `android:exported="false"`, salvo `BootReceiver`, que necesita `exported="true"` para su `intent-filter` de `BOOT_COMPLETED`. Agregar solo los permisos que realmente se usan.

## Verificar

- `./gradlew assembleDebug`.
- Para probar en el emulador: alarmas con una hora a 1 minuto; geofences cambiando la ubicación desde *Extended controls → Location* del emulador. Decirle al usuario cómo probarlo, porque no se puede verificar solo con el build.
