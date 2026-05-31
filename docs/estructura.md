# Estructura del código

[← Volver al README](../README.md)

## Archivos principales

- `app/src/main/java/com/waytolearn/alertadolar/MainActivity.kt`  
  Pantalla principal: selector de moneda, precio actual, métricas 7d y configuración de umbral.

- `app/src/main/java/com/waytolearn/alertadolar/DolarWorker.kt`  
  Worker encadenado por `DailyNotificationScheduler`: obtiene el precio, muestra la **notificación del sistema** y guarda una copia en el **historial interno** (`InAppNotificationStore`), con tipo según hubo movimiento, umbral o error.

- `app/src/main/java/com/waytolearn/alertadolar/DailyNotificationScheduler.kt`  
  Programa el siguiente disparo a las **10:00** o **20:00** locales y pasa la hora prevista al worker (p. ej. para el mantenimiento del domingo por la noche).

- `app/src/main/java/com/waytolearn/alertadolar/InAppNotificationStore.kt`  
  Persistencia JSON del historial interno; en **domingo 20:00** (tras una ejecución programada exitosa con notificación) puede **quitar el registro más antiguo** si la lista tiene más de un elemento.

- `app/src/main/java/com/waytolearn/alertadolar/ExchangeRateRepository.kt`  
  Capa de acceso a datos de tipo de cambio. Intenta primero ExchangeRate-API (si hay clave) y usa Frankfurter v2 como respaldo e histórico.

- `app/src/main/java/com/waytolearn/alertadolar/AppPreferences.kt`  
  Claves centralizadas para `SharedPreferences` (moneda seleccionada y umbral).

- `app/src/main/res/layout/activity_main.xml`  
  Layout principal con el monitor de divisas.

- `app/src/main/res/raw/alerta_dolar.ogg`  
  Sonido personalizado para las notificaciones (formato recomendado: OGG).

- `.github/workflows/release.yml`  
  Workflow de GitHub Actions que compila el APK release firmado y lo publica en Releases.

## Documentación relacionada

- [Configuración del proyecto](configuracion.md) — requisitos y claves de API.
- [Publicar release en GitHub](release-github.md) — secrets, etiquetas y APK automático.
- [Notificaciones](notificaciones.md) — horarios, historial interno y sonido personalizado.
