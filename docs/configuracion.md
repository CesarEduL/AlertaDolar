# Configuración del proyecto

[← Volver al README](../README.md)

## Requisitos

- **Android Studio** (Giraffe o superior).
- **Java 8** o superior.
- SDK de Android instalado (API 35 recomendada).

## Clonar el repositorio

```bash
git clone https://github.com/<tu-usuario>/AlertaDolar.git
cd AlertaDolar
```

Si partes de una carpeta sin historial Git, inicializa el repo y enlaza el remoto:

```bash
git init
git remote add origin https://github.com/<tu-usuario>/AlertaDolar.git
git add .
git commit -m "Initial commit"
git branch -M main
git push -u origin main
```

El archivo **`.gitignore`** en la raíz excluye `local.properties`, cachés de Gradle/Android Studio, firmas (`*.jks`, `*.keystore`) y artefactos habituales; revisa que no añadas claves ni keystores al commit.

Abre la carpeta raíz (`AlertaDolar`) en Android Studio y espera a que sincronice Gradle.

## Configurar claves de API (opcional)

Para mejorar la disponibilidad, el proyecto puede usar **ExchangeRate-API** como fuente primaria y Frankfurter como respaldo.  
La clave **no se incluye en el repositorio**; se inyecta a través de `local.properties` y `BuildConfig`.

1. Abre (o crea) el archivo `local.properties` en la raíz del proyecto:

   ```properties
   sdk.dir=C\:\\Users\\<tu-usuario>\\AppData\\Local\\Android\\Sdk
   EXCHANGE_RATE_API_KEY=TU_CLAVE_AQUI
   ```

2. Si no defines `EXCHANGE_RATE_API_KEY`, la app seguirá funcionando solo con **Frankfurter** (precio actual + histórico).

La misma clave puede pasarse como **variable de entorno** `EXCHANGE_RATE_API_KEY` (por ejemplo en GitHub Actions). Gradle prioriza `local.properties` y, si está vacío, usa el entorno.

> **Nota:** `local.properties` ya está excluido de control de versiones; no subas tu clave al repositorio.

## Firma release en local (opcional)

Para generar un APK release firmado en tu máquina, añade también en `local.properties`:

```properties
RELEASE_KEYSTORE_PATH=C\:\\ruta\\a\\alertadolar-release.jks
RELEASE_KEYSTORE_PASSWORD=tu_contraseña
RELEASE_KEY_ALIAS=alertadolar
RELEASE_KEY_PASSWORD=tu_contraseña
```

En CI/CD (GitHub) estos valores van como **repository secrets**; la guía completa está en [Release en GitHub](release-github.md).

## Siguientes pasos

- [Estructura del código](estructura.md) — conoce los archivos principales del proyecto.
- [Generar el APK](generar-apk.md) — compila e instala la app en un dispositivo.
- [Release en GitHub](release-github.md) — publica el APK en GitHub Releases.
