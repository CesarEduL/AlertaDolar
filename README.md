## AlertaDolar

Aplicación Android para **monitorizar el tipo de cambio USD → PEN** (y otras divisas), mostrar métricas de los últimos 7 días y enviar **notificaciones programadas** cuando el precio baja de un umbral configurado por el usuario.

### Características

- **Monitor de divisas**: selección de moneda base (USD, EUR, GBP, BRL, CLP, …).
- **Precio en soles (PEN)** en tiempo real.
- **Histórico 7 días** con mínimo y máximo usando Frankfurter v2.
- **Umbral configurable**: el usuario indica “notificar cuando baje de…”.
- **Notificaciones en segundo plano** con WorkManager (cada 12 horas) y **sonido personalizado**.

### Tecnologías

- Kotlin + AndroidX (`AppCompat`, `ConstraintLayout`, `Material Components`).
- `WorkManager` para tareas periódicas en segundo plano.
- Consumo de APIs de tipo de cambio:
  - [ExchangeRate-API v6](https://www.exchangerate-api.com/docs) (API primaria, opcional).
  - [Frankfurter v2](https://www.frankfurter.app/docs/) (respaldo y series históricas).

---

## Configuración del proyecto

### 1. Requisitos

- **Android Studio** (Giraffe o superior).
- **Java 8** o superior.
- SDK de Android instalado (API 35 recomendada).

### 2. Clonar el repositorio

```bash
git clone https://github.com/<tu-usuario>/AlertaDolar.git
cd AlertaDolar
```

Abre la carpeta raíz (`AlertaDolar`) en Android Studio y espera a que sincronice Gradle.

### 3. Configurar claves de API (opcional)

Para mejorar la disponibilidad, el proyecto puede usar **ExchangeRate-API** como fuente primaria y Frankfurter como respaldo.  
La clave **no se incluye en el repositorio**; se inyecta a través de `local.properties` y `BuildConfig`.

1. Abre (o crea) el archivo `local.properties` en la raíz del proyecto:

   ```properties
   sdk.dir=C\:\\Users\\<tu-usuario>\\AppData\\Local\\Android\\Sdk
   EXCHANGE_RATE_API_KEY=TU_CLAVE_AQUI
   ```

2. Si no defines `EXCHANGE_RATE_API_KEY`, la app seguirá funcionando solo con **Frankfurter** (precio actual + histórico).

> **Nota:** `local.properties` ya está excluido de control de versiones; no subas tu clave al repositorio.

---

## Estructura principal

- `app/src/main/java/com/waytolearn/alertadolar/MainActivity.kt`  
  Pantalla principal: selector de moneda, precio actual, métricas 7d y configuración de umbral.

- `app/src/main/java/com/waytolearn/alertadolar/DolarWorker.kt`  
  Worker periódico (WorkManager) que consulta el tipo de cambio y envía la notificación si el precio está por debajo del umbral. Configura el **canal de notificación** y el **sonido personalizado**.

- `app/src/main/java/com/waytolearn/alertadolar/ExchangeRateRepository.kt`  
  Capa de acceso a datos de tipo de cambio. Intenta primero ExchangeRate-API (si hay clave) y usa Frankfurter v2 como respaldo e histórico.

- `app/src/main/java/com/waytolearn/alertadolar/AppPreferences.kt`  
  Claves centralizadas para `SharedPreferences` (moneda seleccionada y umbral).

- `app/src/main/res/layout/activity_main.xml`  
  Layout principal con el monitor de divisas.

- `app/src/main/res/raw/alerta_dolar.ogg`  
  Sonido personalizado para las notificaciones (formato recomendado: OGG).

---

## Cómo generar el APK

### Opción 1: Desde Android Studio (recomendada)

1. Abre el proyecto en Android Studio.
2. Selecciona el **Build Variant** `debug` o `release` según necesites.
3. Menú **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. Al finalizar, Android Studio mostrará una notificación; haz clic en **locate** para abrir la carpeta:
   - APK **debug**: `app/build/outputs/apk/debug/app-debug.apk`
   - APK **release**: `app/build/outputs/apk/release/app-release.apk`

Para publicar en tiendas necesitarás firmar el APK de **release** (wizard: **Build → Generate Signed Bundle / APK…**).

### Opción 2: Desde línea de comandos

En la raíz del proyecto:

```bash
./gradlew assembleDebug    # APK debug
./gradlew assembleRelease  # APK release (requiere firma para publicar)
```

Los archivos se generan en:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

---

## Notificaciones y sonido personalizado

- El canal de notificación se define en `DolarWorker.kt` con un `NotificationChannel` (Android 8+).
- El identificador del canal está en `res/values/strings.xml` (`notification_channel_id`).
- El sonido se toma del recurso `raw` `alerta_dolar.ogg` y se asigna al canal usando `AudioAttributes` (`USAGE_NOTIFICATION`).
- En dispositivos **anteriores a Android 8**, el sonido se aplica directamente al `NotificationCompat.Builder`.

Si cambias el sonido o el comportamiento del canal, recuerda que Android **no actualiza** canales ya existentes: puede ser necesario **desinstalar la app** del dispositivo o cambiar el `notification_channel_id` para forzar la creación de un canal nuevo.

---

## Licencia

Añade aquí la licencia de tu preferencia (por ejemplo, MIT, Apache-2.0 o GPL). Si el proyecto es privado, puedes omitir esta sección o indicar que todos los derechos están reservados.

