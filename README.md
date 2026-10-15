# LogitNotes 🌿
INSTALAR:
https://www.mediafire.com/file/ss7fsdv2nvwl70e/app-debug.apk/file

Bóveda de notas personal, 100% privada, de código abierto (FLOSS) y fuera de línea (offline-first) con cifrado AES-256-GCM y autenticación biométrica para Android.

## Características

- 🛡️ **Cifrado Fuerte**: Cifrado autenticado AES-256-GCM con derivación de clave maestra PBKDF2 (100.000 iteraciones).
- 🔒 **Almacenamiento 100% Local**: No utiliza servidores ni servicios en la nube. Tus datos residen exclusivamente en el almacenamiento protegido de tu dispositivo.
- 🖐️ **Autenticación Biométrica**: Desbloqueo rápido mediante huella dactilar o reconocimiento facial (`BiometricPrompt` nativo).
- 📝 **Markdown Completo**: Visualización y edición en Markdown estándar con soporte para checklists, encabezados y estilos.
- 📎 **Archivos Adjuntos Cifrados**: Guarda fotos y documentos dentro de la bóveda de forma segura.
- 📦 **Exportación Fácil**: Exporta notas en `.md` o la bóveda completa en formato `.zip`.
- 🍃 **Diseño Eco-Minimalista**: Interfaz limpia, ágil y de bajo consumo de batería y memoria RAM.

## Compilación desde código fuente

El proyecto utiliza Gradle estándar y es 100% compatible con los requisitos de compilación de **F-Droid**:

### Generar APK de depuración (Debug)
```bash
gradle assembleDebug
```
El archivo APK se genera en:
`app/build/outputs/apk/debug/app-debug.apk`

### Generar APK de producción (Release)
```bash
gradle assembleRelease
```
El archivo APK se genera en:
`app/build/outputs/apk/release/app-release-unsigned.apk`

## Estructura para F-Droid

- Licencia: Apache-2.0 (`LICENSE`)
- Metadatos Fastlane: `fastlane/metadata/android/`
- Sin bibliotecas binarias no libres ni SDKs propietarios de rastreo.
