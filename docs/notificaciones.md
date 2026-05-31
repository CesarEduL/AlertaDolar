# Notificaciones y sonido personalizado

[← Volver al README](../README.md)

## Horario

Las alertas automáticas se programan **dos veces al día**, a las **10:00** y **20:00** en la **zona horaria local del dispositivo** (no UTC fijo). La lógica está en `DailyNotificationScheduler.kt` (`targetHours`: 10 y 20) y usa un `OneTimeWorkRequest` encadenado para el siguiente intervalo.

## Historial interno y domingo por la noche

Las mismas ejecuciones pueden dejar un texto en la bandeja interna de la app. **Solo los domingos**, cuando la corrida corresponde al turno de las **20:00** y la notificación se envió con éxito, la app **elimina el registro más antiguo** del historial interno **si había más de una entrada**; así el listado no crece sin límite. El sábado por la noche **no** aplica este borrado.

## Canal y audio

- El canal de notificación se define en `CurrencyNotificationHelper` / recursos asociados (Android 8+).
- El identificador del canal está en `res/values/strings.xml` (`notification_channel_id`).
- El sonido se toma del recurso `raw` `alerta_dolar.ogg` y se asigna al canal usando `AudioAttributes` (`USAGE_NOTIFICATION`).
- En dispositivos **anteriores a Android 8**, el sonido se aplica directamente al `NotificationCompat.Builder`.

Si cambias el sonido o el comportamiento del canal, recuerda que Android **no actualiza** canales ya existentes: puede ser necesario **desinstalar la app** del dispositivo o cambiar el `notification_channel_id` para forzar la creación de un canal nuevo.

## Documentación relacionada

- [Estructura del código](estructura.md) — archivos implicados (`DolarWorker`, `DailyNotificationScheduler`, `InAppNotificationStore`).
