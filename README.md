# ESCOM Orienta Móviles

Aplicación Android para estudiantes de ESCOM-IPN: consulta el mapa del plantel, organiza tu horario y encuentra información sobre trámites y servicios escolares.

Proyecto académico de Móviles basado en ESCOM Orienta, adaptado para continuar su desarrollo en equipo **sin módulo de chatbot ni dependencias de IA**.

## Funcionalidades

- **Inicio:** accesos rápidos y consulta de la clase actual o siguiente.
- **Mapa:** búsqueda de salones y espacios, rutas entre pisos, distancias e indicaciones paso a paso.
- **Horario:** importación de PDF, captura y edición de clases, personalización y exportación a calendario `.ics`.
- **Trámites y servicios:** catálogo local de 46 recursos, con filtros para trámites, servicios, becas, deportes y cultura.
- **Recordatorios:** avisos de clases y reprogramación tras reinicios o cambios de hora.
- **Ajustes:** tema, alias, preferencias del mapa, notificaciones y gestión de datos locales.

La navegación principal contiene **Inicio, Mapa, Horario y Trámites**. Los enlaces externos y el envío de archivos se abren con las aplicaciones disponibles en el dispositivo.

## Tecnologías y requisitos

- Kotlin y Jetpack Compose con Material 3.
- Room para persistencia local y PDFBox Android para lectura de PDF.
- Android Studio compatible con Android Gradle Plugin **9.2.1**.
- Android SDK **36** y un dispositivo o emulador con **Android 10 / API 29** o superior.
- Gradle Wrapper **9.4.1** incluido; no requiere instalar Gradle por separado.
- El daemon de Gradle está configurado para **JDK 21** en `gradle/gradle-daemon-jvm.properties`.

## Empezar

```bash
git clone https://github.com/Isra921/App-M-viles.git
cd App-M-viles
```

1. En Android Studio, selecciona **Open** y abre la carpeta raíz que contiene `settings.gradle.kts`.
2. Instala Android SDK 36 si el IDE lo solicita y espera a que finalice la sincronización de Gradle. La primera sincronización requiere conexión para descargar dependencias.
3. Configura la ubicación de tu SDK desde Android Studio. El archivo `local.properties` es propio de cada equipo y está excluido de Git.
4. Selecciona el módulo `app` y un emulador o teléfono con depuración USB.
5. Pulsa **Run**. Si no aparece una configuración, crea una de tipo **Android App**, selecciona el módulo `app` y deja **Default Activity**.

La app usa el identificador `com.escom.escomorienta.moviles`, por lo que puede instalarse junto a la versión original y mantiene sus datos separados.

## Compilación y pruebas

Desde PowerShell en la raíz del proyecto:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

Para pruebas instrumentadas con un dispositivo o emulador conectado:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

En macOS o Linux, usa `bash ./gradlew` en lugar de `.\gradlew.bat`.

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`.

**Estado de validación:** la actualización se revisó estáticamente, incluyendo referencias de navegación y recursos JSON/XML. No se ejecutaron compilación ni pruebas durante su preparación; queda pendiente validarla en Android Studio y en dispositivo.

## Organización

```text
app/src/main/
├── java/com/escom/escomorienta/
│   ├── data/            # Repositorios, persistencia, mapas y procesamiento de horarios
│   ├── domain/          # Reglas de navegación, trámites y recordatorios
│   ├── navigation/      # Rutas y destinos de la aplicación
│   ├── notifications/   # Programación y recepción de avisos
│   └── ui/              # Pantallas, componentes y temas
├── assets/
│   ├── maps/            # Lugares, accesos y grafos del plantel
│   └── institutional/   # Catálogo de trámites y servicios
└── res/                 # Imágenes, iconos y recursos Android
```

Las pruebas locales están en `app/src/test` y las instrumentadas en `app/src/androidTest`.

## Trabajo en equipo

Cada integrante con permisos de escritura puede crear una rama para su cambio:

```bash
git switch main
git pull --ff-only origin main
git switch -c mejora/nombre-del-cambio
```

Después de modificar y revisar los archivos:

```bash
git add ruta/del/archivo
git commit -m "Describe el cambio realizado"
git push -u origin mejora/nombre-del-cambio
```

Abre un **Pull Request hacia `main`** indicando qué cambió y cómo lo probaste. Para subir ramas directamente, el propietario debe agregar a cada compañero como colaborador desde la configuración del repositorio. Sin permisos de escritura, pueden trabajar mediante un fork y enviar un Pull Request.

No subas `local.properties`, cachés, compilaciones, respaldos, credenciales ni claves de firma. El `.gitignore` ya excluye los archivos locales habituales. Mantén esta versión sin chatbot al incorporar cambios del proyecto original.

## Versión actual

**1.1 · versionCode 2.** Incluye la actualización del 10 de noviembre de 2026: mejoras de mapas y navegación, exportación de horarios, recordatorios, catálogo institucional y ajustes visuales. Se excluyen el chatbot, ONNX, las contribuciones de conversaciones y el enlace de actualización de la app original.