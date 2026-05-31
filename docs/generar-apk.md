# Generar el APK

[← Volver al README](../README.md)

## Opción 1: Desde Android Studio (recomendada)

1. Abre el proyecto en Android Studio.
2. Selecciona el **Build Variant** `debug` o `release` según necesites.
3. Menú **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. Al finalizar, Android Studio mostrará una notificación; haz clic en **locate** para abrir la carpeta:
   - APK **debug**: `app/build/outputs/apk/debug/app-debug.apk`
   - APK **release**: `app/build/outputs/apk/release/AlertaDolar-v{versionName}-release.apk` (p. ej. `AlertaDolar-v1.0-release.apk`)

Para publicar en tiendas o distribuir release firmado, configura la firma en `local.properties` (ver abajo) o usa **Build → Generate Signed Bundle / APK…**.

## Opción 2: Desde línea de comandos

En la raíz del proyecto:

```bash
./gradlew assembleDebug    # APK debug
./gradlew assembleRelease  # APK release (firmado si hay keystore configurado)
```

Los archivos se generan en:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/AlertaDolar-v1.0-release.apk` (según `versionName` en `app/build.gradle.kts`)

## Firma release en local (opcional)

Añade en `local.properties` (no se sube al repo):

```properties
RELEASE_KEYSTORE_PATH=C\:\\ruta\\a\\alertadolar-release.jks
RELEASE_KEYSTORE_PASSWORD=tu_contraseña
RELEASE_KEY_ALIAS=alertadolar
RELEASE_KEY_PASSWORD=tu_contraseña
```

Gradle también acepta las mismas claves como **variables de entorno** (como en GitHub Actions).

## Opción 3: Release automático en GitHub

Para publicar el APK en **GitHub Releases** sin compilar a mano, sigue [Publicar release en GitHub](release-github.md).

## Documentación relacionada

- [Configuración del proyecto](configuracion.md) — requisitos previos y clonado del repo.
