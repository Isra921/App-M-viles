# ESCOM Orienta Móviles

Versión independiente para el proyecto de Móviles, basada en ESCOM Orienta.

## Funciones
- Inicio con accesos rápidos y la clase actual o siguiente.
- Mapa del plantel, búsqueda de lugares, rutas e indicaciones paso a paso con distancias calibradas.
- Horario: importación de PDF, edición, personalización, tarjetas de resumen y exportación a calendario (.ics).
- Catálogo de 46 trámites, servicios, becas y actividades, con filtros y fichas detalladas.
- Ajustes de apariencia, alias, recordatorios y datos locales.
- Diagnósticos locales de errores.

Se retiraron el chatbot, sus pantallas, historial, contribuciones, motor semántico, modelos, catálogos, dependencias ONNX y pruebas exclusivas. Los diagnósticos de mapa y ajustes ya no dependen del motor de conversación.

La navegación inferior contiene Inicio, Mapa, Horario y Trámites. Se eliminó el enlace de actualización que descargaba la aplicación original.

## Abrir y compilar
Abrir esta carpeta en Android Studio. Requiere JDK 17 o compatible, Android SDK 36 y las dependencias declaradas en Gradle.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Identificador: `com.escom.escomorienta.moviles`. Puede coexistir con la app original y mantiene sus datos separados. El namespace del código se conserva para reducir cambios innecesarios.

La copia no incluye historial Git, cachés, compilaciones previas ni reportes de evaluación del Trabajo Terminal. El proyecto original no se modifica. `local.properties` contiene la ruta local del SDK y debe ajustarse en otros equipos.
## Actualización del 10 de noviembre de 2026

Versión de Móviles: 1.1 (versionCode 2). Se incorporaron los cambios disponibles en la copia local de `Trabajo Terminal/ESCOM_Orienta`:

- Mapas y grafos actualizados, calibración de distancias, navegación paso a paso y preferencias del mapa.
- Exportación de horario a iCalendar, tarjetas de resumen, ajustes en importación de PDF y persistencia del horario.
- Cálculo y caducidad de recordatorios, reprogramación tras reinicios, cambios de hora y permisos.
- Catálogo institucional ampliado a 46 recursos, filtros por tipo y detalles de servicios, becas, deportes y cultura.
- Ajustes visuales, paletas y dependencias de animación. Se mantienen Inicio, Mapa, Horario y Trámites.
- Pruebas funcionales de mapa, notificaciones y catálogo actualizadas; la prueba de navegación excluye las acciones del módulo retirado.

No se incorporan el chatbot, sus modelos, catálogos, pantallas, contribuciones ni dependencias ONNX. Tampoco se incorpora el enlace de descarga de la app original. Se conservan el identificador independiente y los datos locales de Móviles.

Respaldo de los archivos reemplazados y listado de cambios: `backups/pre-update-20261110-073440/`. Se omiten los benchmarks académicos que dependen de evidencias externas.

## Validación

Se revisaron estáticamente las referencias de navegación, dependencias retiradas, 57 archivos JSON, 19 archivos XML y las rutas de los 46 recursos institucionales. Se ajustó FileProvider para compartir el calendario exportado.

No se ejecutaron Gradle, compilación ni pruebas durante esta actualización, por indicación del usuario. Pendiente probar en Android Studio: inicio y ajustes, mapa y rutas entre pisos, importación/exportación del horario, recordatorios y navegación por los trámites.