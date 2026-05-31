# AlertaDolar

Aplicación Android para **monitorizar el tipo de cambio USD → PEN** (y otras divisas), mostrar métricas de los últimos 7 días y enviar **notificaciones programadas** cuando el precio baja de un umbral configurado por el usuario.

## Características

- **Monitor de divisas**: selección de moneda base (USD, EUR, GBP, BRL, CLP, …).
- **Precio en soles (PEN)** en tiempo real.
- **Histórico 7 días** con mínimo y máximo usando Frankfurter v2.
- **Umbral configurable**: el usuario indica “notificar cuando baje de…”.
- **Notificaciones programadas en segundo plano** con WorkManager a las **10:00** y **20:00** (hora local del dispositivo), con **sonido personalizado**.
- **Bandeja interna** de historial con **filtro por tipo** (cambio de precio, sobre/debajo del umbral, errores).
- **Mantenimiento del historial**: cada **domingo**, tras la notificación de las **20:00**, se elimina automáticamente **un registro** (el más antiguo), si hay más de una entrada — borrado progresivo para no saturar la lista.

## Tecnologías

- Kotlin + AndroidX (`AppCompat`, `ConstraintLayout`, `Material Components`).
- `WorkManager` para tareas periódicas en segundo plano.
- Consumo de APIs de tipo de cambio:
  - [ExchangeRate-API v6](https://www.exchangerate-api.com/docs) (API primaria, opcional).
  - [Frankfurter v2](https://www.frankfurter.app/docs/) (respaldo y series históricas).

## Documentación

| Guía | Contenido |
|------|-----------|
| [Configuración del proyecto](docs/configuracion.md) | Requisitos, clonado del repo y claves de API |
| [Estructura del código](docs/estructura.md) | Archivos principales y responsabilidades |
| [Generar el APK](docs/generar-apk.md) | Build desde Android Studio o línea de comandos |
| [Release en GitHub](docs/release-github.md) | Secrets, etiquetas y APK automático en Releases |
| [Notificaciones](docs/notificaciones.md) | Horarios, historial interno, canal y sonido |

## Licencia

Añade aquí la licencia de tu preferencia (por ejemplo, MIT, Apache-2.0 o GPL). Si el proyecto es privado, puedes omitir esta sección o indicar que todos los derechos están reservados.
