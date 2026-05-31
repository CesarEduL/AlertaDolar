# Publicar un release en GitHub con el APK

[← Volver al README](../README.md)

Esta guía explica **paso a paso** qué hacer en GitHub para que el repositorio compile automáticamente un APK firmado y lo publique en **Releases** al crear una etiqueta `v*`.

El workflow ya está en [`.github/workflows/release.yml`](../.github/workflows/release.yml).

---

## Resumen del flujo

1. Generas un **keystore** de firma (una sola vez).
2. Guardas los datos sensibles como **Secrets** en GitHub.
3. Subes el código y creas una **etiqueta** (p. ej. `v1.0.0`).
4. GitHub Actions compila, firma y adjunta `app-release.apk` al release.

---

## Paso 1: Crear el keystore de firma (solo la primera vez)

En tu máquina, desde cualquier carpeta:

```bash
keytool -genkeypair -v \
  -keystore alertadolar-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias alertadolar
```

Te pedirá contraseña del keystore, contraseña de la clave y datos del certificado. **Guarda todo en un lugar seguro**; sin el keystore no podrás publicar actualizaciones con la misma firma.

> **Importante:** El archivo `.jks` **no debe subirse al repositorio**. Ya está excluido en `.gitignore`.

---

## Paso 2: Codificar el keystore en Base64

GitHub Actions necesita el keystore como secret en texto. En **Git Bash** (Windows) o terminal Linux/macOS:

```bash
base64 -w 0 alertadolar-release.jks
```

En **macOS**:

```bash
base64 -i alertadolar-release.jks | tr -d '\n'
```

Copia **toda** la salida (una línea larga). La usarás en el paso 3.

---

## Paso 3: Añadir secrets en GitHub

1. Abre tu repositorio en GitHub.
2. Ve a **Settings** → **Secrets and variables** → **Actions**.
3. Pulsa **New repository secret** y crea cada uno de estos:

| Nombre del secret | Obligatorio | Valor |
|-------------------|-------------|-------|
| `RELEASE_KEYSTORE_BASE64` | Sí | Salida completa del paso 2 |
| `RELEASE_KEYSTORE_PASSWORD` | Sí | Contraseña del keystore |
| `RELEASE_KEY_ALIAS` | Sí | Alias usado al crear el keystore (p. ej. `alertadolar`) |
| `RELEASE_KEY_PASSWORD` | Sí | Contraseña de la clave (suele ser la misma que la del keystore) |
| `EXCHANGE_RATE_API_KEY` | No | Clave de [ExchangeRate-API](https://www.exchangerate-api.com/). Si no la pones, el APK usará solo Frankfurter |

Los nombres deben coincidir **exactamente** con la tabla (mayúsculas y guiones bajos).

---

## Paso 4: Subir el código con el workflow

Asegúrate de que en tu rama principal están:

- `.github/workflows/release.yml`
- Los cambios de `app/build.gradle.kts` que leen variables de entorno

```bash
git add .
git commit -m "Añadir workflow de release con APK"
git push origin main
```

(Sustituye `main` por el nombre de tu rama principal si es distinto.)

---

## Paso 5: Crear una etiqueta y disparar el release

Cada vez que quieras publicar una versión nueva:

### Opción A — Desde la terminal (recomendada)

1. Actualiza la versión en `app/build.gradle.kts` si hace falta (`versionCode` y `versionName`).
2. Commit y push de esos cambios.
3. Crea y sube la etiqueta:

```bash
git tag v1.0.0
git push origin v1.0.0
```

El prefijo **`v`** es obligatorio: el workflow solo se activa con etiquetas que empiecen por `v` (`v1.0.0`, `v1.0.1`, etc.).

### Opción B — Desde la interfaz de GitHub

1. **Releases** → **Draft a new release**.
2. En **Choose a tag**, escribe `v1.0.0` y créala sobre `main`.
3. Publica el release.

Si la etiqueta se crea al publicar, también se dispara el workflow.

### Opción C — Ejecución manual

1. Ve a la pestaña **Actions** del repo.
2. Elige **Release APK** en la lista de workflows.
3. **Run workflow** → **Run workflow**.

Útil para probar sin crear una etiqueta nueva (aunque el release usará el nombre de la rama/ref actual).

---

## Paso 6: Seguir el build y descargar el APK

1. En GitHub, abre **Actions** y entra en la ejecución **Release APK**.
2. Si termina en verde, ve a **Releases** en el repo.
3. En el release verás el asset **`app-release.apk`**. Descárgalo e instálalo en el dispositivo (habilita “Orígenes desconocidos” si Android lo pide).

---

## Cómo llega la API key al APK

En el build de CI, el workflow pasa `EXCHANGE_RATE_API_KEY` como variable de entorno. Gradle la lee y la compila en `BuildConfig`:

```kotlin
BuildConfig.EXCHANGE_RATE_API_KEY
```

En local puedes usar lo mismo vía `local.properties`:

```properties
EXCHANGE_RATE_API_KEY=tu_clave
RELEASE_KEYSTORE_PATH=C\:\\ruta\\alertadolar-release.jks
RELEASE_KEYSTORE_PASSWORD=tu_contraseña
RELEASE_KEY_ALIAS=alertadolar
RELEASE_KEY_PASSWORD=tu_contraseña
```

> **Seguridad:** La clave queda **dentro del APK**. Cualquiera puede extraerla descompilando el binario. No la trates como secreto absoluto; si no la configuras, la app sigue funcionando con Frankfurter.

---

## Solución de problemas

| Problema | Qué revisar |
|----------|-------------|
| Workflow no arranca | La etiqueta debe ser `v*` (p. ej. `v1.0.0`, no `1.0.0`). |
| Error `RELEASE_KEYSTORE_BASE64` | Falta el secret o el Base64 está truncado. |
| Error de firma | Alias o contraseñas incorrectas en los secrets. |
| APK sin ExchangeRate-API | Secret `EXCHANGE_RATE_API_KEY` vacío o no creado (Frankfurter sigue activo). |
| Canal de Actions falla en SDK | Revisa el log; el workflow instala `platforms;android-35` y `build-tools;35.0.0`. |

---

## Documentación relacionada

- [Configuración del proyecto](configuracion.md) — claves API en local.
- [Generar el APK](generar-apk.md) — build manual desde Android Studio o Gradle.
