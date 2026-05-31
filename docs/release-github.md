# Publicar un release en GitHub con el APK

[← Volver al README](../README.md)

Esta guía explica **paso a paso** qué hacer en GitHub para que el repositorio compile automáticamente un APK firmado y lo publique en **Releases** al crear una etiqueta `v*`.

El workflow ya está en [`.github/workflows/release.yml`](../.github/workflows/release.yml).

---

## Resumen del flujo

1. Generas un **keystore** de firma (una sola vez).
2. Guardas los datos sensibles como **Secrets** en GitHub (no uses `.env` en el repo).
3. Subes el código y creas una **etiqueta** (p. ej. `v1.0.0`).
4. GitHub Actions compila, firma y adjunta `app-release.apk` al release.

---

## ¿Dónde van las llaves? (no hay `.env`)

Este proyecto **no usa un archivo `.env`**. Las claves se guardan así:

| Entorno | Dónde | Qué va ahí |
|---------|-------|------------|
| **Tu PC (desarrollo local)** | `local.properties` en la raíz del proyecto | API key, ruta del `.jks`, contraseñas de firma |
| **GitHub Actions (CI)** | **Settings → Secrets and variables → Actions** | Los mismos valores, como repository secrets |
| **APK compilado** | Dentro de `BuildConfig` | La API key queda embebida al hacer build |

Gradle lee primero `local.properties` y, si falta una clave, busca la **variable de entorno** con el mismo nombre (por ejemplo `EXCHANGE_RATE_API_KEY`).

`local.properties` y los archivos `.jks` **no se suben al repositorio** (están en `.gitignore`).

### Ejemplo de `local.properties` (solo en tu máquina)

Crea el archivo en la raíz del proyecto (`AlertaDolar/local.properties`):

```properties
sdk.dir=C\:\\Users\\cesar\\AppData\\Local\\Android\\Sdk
EXCHANGE_RATE_API_KEY=tu_clave_de_exchangerate_api

RELEASE_KEYSTORE_PATH=C\:\\Users\\cesar\\AndroidStudioProjects\\AlertaDolar\\alertadolar-release.jks
RELEASE_KEYSTORE_PASSWORD=la_contraseña_que_elegiste
RELEASE_KEY_ALIAS=alertadolar
RELEASE_KEY_PASSWORD=la_contraseña_que_elegiste
```

Sustituye las rutas y contraseñas por las tuyas. Si no pones `EXCHANGE_RATE_API_KEY`, la app usará solo Frankfurter.

---

## Paso 1: Crear el keystore de firma (solo la primera vez)

En tu máquina, abre **PowerShell** en la carpeta donde quieras guardar el archivo (por ejemplo, la raíz del proyecto).

### PowerShell — una sola línea (recomendado)

```powershell
keytool -genkeypair -v -keystore alertadolar-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias alertadolar
```

### PowerShell — multilínea

En PowerShell la continuación de línea es con **`` ` ``** (backtick), no con `\`:

```powershell
keytool -genkeypair -v `
  -keystore alertadolar-release.jks `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -alias alertadolar
```

### Si `keytool` no se reconoce

Usa la ruta del JDK que trae Android Studio:

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore alertadolar-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias alertadolar
```

### Git Bash / Linux / macOS

```bash
keytool -genkeypair -v -keystore alertadolar-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias alertadolar
```

### Qué te preguntará `keytool` y de dónde salen las contraseñas

**No hay una contraseña predefinida.** Tú la **inventas y escribes** cuando el comando lo pide. Esa misma contraseña irá después a los secrets de GitHub.

Orden típico (puede aparecer en español o inglés según tu JDK):

| Pregunta en pantalla | Qué hacer |
|----------------------|-----------|
| `Introduzca la contraseña del almacén de claves` / `Enter keystore password` | Escribe la contraseña que **tú eliges** (ej. una fecha o frase que recuerdes) |
| `Volver a escribir la contraseña nueva` / `Re-enter password` | Repite la misma contraseña |
| Nombre, unidad, organización, ciudad, provincia, país | Tus datos; Enter acepta el valor por defecto `[Unknown]` |
| Código de país de **dos letras** | Usa **`PE`** para Perú (no `51`, que es el prefijo telefónico) |
| `¿Es correcto CN=...?` / `Is CN=... correct?` | Escribe **`si`** o **`yes`** |
| `Enter key password for <alertadolar>` | Pulsa **Enter** para usar **la misma** contraseña del keystore (recomendado) |

Si todo va bien, verás algo como:

```text
Generando par de claves RSA de 2,048 bits...
[Almacenando alertadolar-release.jks]
```

Comprueba que el archivo existe:

```powershell
dir alertadolar-release.jks
```

### Qué valor va en cada secret de GitHub

| Secret en GitHub | De dónde sale | Ejemplo |
|------------------|---------------|---------|
| `RELEASE_KEYSTORE_PASSWORD` | Lo que escribiste al crear el keystore | La contraseña que elegiste |
| `RELEASE_KEY_ALIAS` | El `-alias` del comando | `alertadolar` |
| `RELEASE_KEY_PASSWORD` | Pregunta final de `keytool`; si pulsaste Enter, **igual** que la del keystore | La misma contraseña del keystore |

**Guarda el `.jks` y las contraseñas en un lugar seguro.** Sin ellos no podrás firmar actualizaciones futuras con la misma identidad.

> **Importante:** El archivo `.jks` **no debe subirse al repositorio**. Ya está excluido en `.gitignore`.

---

## Paso 2: Codificar el keystore en Base64

GitHub Actions necesita el keystore como secret en texto.

### PowerShell (Windows)

Copia el Base64 al portapapeles (ejecuta desde la carpeta donde está el `.jks`):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("alertadolar-release.jks")) | Set-Clipboard
```

O imprímelo en pantalla:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("alertadolar-release.jks"))
```

### Git Bash / Linux

```bash
base64 -w 0 alertadolar-release.jks
```

### macOS

```bash
base64 -i alertadolar-release.jks | tr -d '\n'
```

Copia **toda** la salida (una línea larga). La pegarás en el secret `RELEASE_KEYSTORE_BASE64` del paso 3.

---

## Paso 3: Añadir secrets en GitHub

1. Abre tu repositorio en GitHub.
2. Ve a **Settings** → **Secrets and variables** → **Actions**.
3. Pulsa **New repository secret** y crea cada uno de estos:

| Nombre del secret | Obligatorio | Valor |
|-------------------|-------------|-------|
| `RELEASE_KEYSTORE_BASE64` | Sí | Salida completa del paso 2 |
| `RELEASE_KEYSTORE_PASSWORD` | Sí | La contraseña que **tú escribiste** al crear el keystore |
| `RELEASE_KEY_ALIAS` | Sí | `alertadolar` (el `-alias` del comando) |
| `RELEASE_KEY_PASSWORD` | Sí | La misma que `RELEASE_KEYSTORE_PASSWORD` si pulsaste Enter al final del paso 1 |
| `EXCHANGE_RATE_API_KEY` | No | Clave de [ExchangeRate-API](https://www.exchangerate-api.com/). Si no la pones, el APK usará solo Frankfurter |

Los nombres deben coincidir **exactamente** con la tabla (mayúsculas y guiones bajos).

> **No crees un `.env` en el repo** para esto. En GitHub solo existen los **repository secrets**; el workflow los pasa como variables de entorno durante el build.

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

> **Importante:** Una etiqueta queda fijada al **commit** en el que se creó. Si corriges el workflow en `main` pero **Re-run** un release antiguo o la etiqueta no se movió, Actions seguirá usando el YAML viejo. Borra y vuelve a crear la etiqueta sobre el último `main`, o publica `v1.0.1`:

```bash
git push origin main
git tag -d v1.0.0
git push origin :refs/tags/v1.0.0
git tag v1.0.0
git push origin v1.0.0
```

(O crea directamente `v1.0.1` si ya publicaste `v1.0.0` en GitHub Releases.)

### Opción B — Desde la interfaz de GitHub

1. En el repo, ve a **Releases** → **Draft a new release** (o **New release**).
2. **Tag:** escribe `v1.0.0` y elige **Create new tag: v1.0.0 on publish** (o créala sobre `main` si ya existe el selector).
3. **Target:** deja **`main`** (o la rama desde la que quieres publicar).
4. **Release title:** título visible del release (ejemplo abajo).
5. **Release notes:** descripción en Markdown (ejemplo abajo). Opcionalmente pulsa **Generate release notes** para que GitHub añada commits/PRs recientes; puedes combinarlo con el texto sugerido.
6. **No hace falta subir el APK a mano** en “Attach binaries”: el workflow **Release APK** lo adjuntará cuando termine (si los secrets están configurados).
7. Pulsa **Publish release**.

Al publicar, se crea la etiqueta `v1.0.0` y se dispara el workflow. Cuando Actions termine en verde, recarga la página del release: debería aparecer **`app-release.apk`**.

#### Texto sugerido — Release title

Copia y pega (cambia la versión si publicas otra):

```text
AlertaDolar v1.0.0
```

#### Texto sugerido — Release notes

Copia y pega en el cuadro **Describe this release** (pestaña **Write**):

```markdown
## AlertaDolar v1.0.0

Primera versión publicada de **AlertaDolar**: monitor de tipo de cambio a soles (PEN) con alertas programadas.

### Descarga e instalación

1. Descarga **`app-release.apk`** en la sección *Assets* de este release (aparece cuando termina el workflow de GitHub Actions).
2. En el teléfono Android, abre el archivo e instala la app.
3. Si el sistema lo pide, permite instalar desde “orígenes desconocidos” o desde el navegador/gestor de archivos que uses.

### Qué incluye esta versión

- Monitor de divisas (USD, EUR, GBP, BRL, CLP, …) con precio en **PEN**.
- Histórico de **7 días** (mínimo y máximo).
- **Umbral configurable**: notificación cuando el precio baje del valor indicado.
- Alertas automáticas a las **10:00** y **20:00** (hora local), con sonido personalizado.
- Bandeja interna de historial con filtro por tipo de evento.

### Requisitos

- Android **7.0** o superior (API 24+).

### Notas

- La app puede usar Frankfurter como fuente de tipos de cambio; ExchangeRate-API es opcional según cómo se haya compilado el APK.
- Para reportar problemas, abre un issue en el repositorio.
```

Para versiones siguientes (`v1.0.1`, `v1.1.0`, …), cambia el número en el título y en el encabezado de las notas, y añade una sección **Cambios** con lo nuevo, por ejemplo:

```markdown
### Cambios

- Descripción del cambio 1
- Descripción del cambio 2
```

### Opción C — Ejecución manual

1. Ve a la pestaña **Actions** del repo.
2. Elige **Release APK** en la lista de workflows.
3. **Run workflow** → **Run workflow**.

Útil para probar sin crear una etiqueta nueva.

---

## Paso 6: Seguir el build y descargar el APK

1. En GitHub, abre **Actions** y entra en la ejecución **Release APK**.
2. Si termina en verde, ve a **Releases** en el repo.
3. En el release verás el asset **`app-release.apk`**. Descárgalo e instálalo en el dispositivo (habilita “Orígenes desconocidos” si Android lo pide).

---

## Cómo llega la API key al APK

En el build de CI, el workflow pasa `EXCHANGE_RATE_API_KEY` como variable de entorno. Gradle la lee y la compila en:

```kotlin
BuildConfig.EXCHANGE_RATE_API_KEY
```

En local, el mismo valor va en `local.properties` (ver sección [¿Dónde van las llaves?](#dónde-van-las-llaves-no-hay-env)).

> **Seguridad:** La clave queda **dentro del APK**. Cualquiera puede extraerla descompilando el binario. No la trates como secreto absoluto; si no la configuras, la app sigue funcionando con Frankfurter.

---

## Checklist rápido

- [ ] `alertadolar-release.jks` creado y guardado en lugar seguro
- [ ] Contraseña del keystore anotada (la que **tú** escribiste, no viene del proyecto)
- [ ] Secret `RELEASE_KEYSTORE_BASE64` en GitHub
- [ ] Secrets `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`
- [ ] Secret `EXCHANGE_RATE_API_KEY` (opcional)
- [ ] Código con el workflow subido a GitHub
- [ ] Etiqueta `v1.0.0` (o similar) creada y pusheada
- [ ] APK descargado desde **Releases**

---

## Solución de problemas

| Problema | Qué revisar |
|----------|-------------|
| `-keyalg` no se reconoce en PowerShell | Pegaste solo parte del comando; debe empezar por `keytool` (ver paso 1). |
| `keytool` no se reconoce | Usa la ruta completa de Android Studio (paso 1). |
| `./gradlew: Permission denied` (exit 126) | Suele pasar en Windows o si la **etiqueta apunta a un commit antiguo** (sin el fix del workflow). Sube el último código, **recrea la etiqueta** sobre `main` (ver abajo) o usa **Run workflow** desde Actions. El workflow usa `bash gradlew` para evitar este error. |
| Workflow no arranca | La etiqueta debe ser `v*` (p. ej. `v1.0.0`, no `1.0.0`). |
| Error `RELEASE_KEYSTORE_BASE64` | Falta el secret o el Base64 está truncado. |
| Error de firma | Alias o contraseñas incorrectas en los secrets. |
| APK sin ExchangeRate-API | Secret `EXCHANGE_RATE_API_KEY` vacío o no creado (Frankfurter sigue activo). |
| Canal de Actions falla en SDK | Revisa el log; el workflow instala `platforms;android-35` y `build-tools;35.0.0`. |

---

## Documentación relacionada

- [Configuración del proyecto](configuracion.md) — claves API y firma en `local.properties`.
- [Generar el APK](generar-apk.md) — build manual desde Android Studio o Gradle.
