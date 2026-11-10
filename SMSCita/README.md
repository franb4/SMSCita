# CitaSMS — primera versión (proyecto Android)

Aplicación personal para Android que analiza SMS localmente y propone citas para añadir a Google Calendar.

## Qué incluye
- Lectura inicial del historial SMS con permiso `READ_SMS`.
- Recepción de SMS nuevos mediante `SMS_RECEIVED_ACTION` y permiso `RECEIVE_SMS`.
- Detección heurística local de mensajes que parecen contener citas.
- Bandeja de revisión para confirmar/corregir los datos.
- Apertura del formulario de creación de evento de Calendar mediante `ACTION_INSERT`. El usuario revisa y guarda el evento; la app no modifica el calendario silenciosamente.
- Deduplicación básica por identificador/hash del mensaje.

## Limitaciones importantes de esta primera versión
- La detección es heurística, no una IA: puede pasar por alto citas o marcar mensajes que no lo sean.
- La extracción de fechas está pensada para formatos habituales en español (`09/11/2026`, `9-11-26`, `9 de noviembre de 2026`) y horas (`10:30`, `a las 10`).
- No interpreta todavía de forma fiable fechas relativas como “mañana”, “el próximo martes” ni cambios/cancelaciones complejos.
- Android y algunos fabricantes pueden limitar la ejecución en segundo plano. Conviene probar la recepción con el teléfono desbloqueado y después con la pantalla apagada.
- Para leer SMS antiguos y recibir los nuevos se solicitan permisos sensibles. Esta app está pensada para instalación privada mediante APK, no se garantiza que Google Play la admita.
- La app no envía SMS ni contenido a servidores.

## Abrir el proyecto
1. Instala Android Studio en un ordenador.
2. Descomprime este ZIP.
3. En Android Studio selecciona **Open** y elige la carpeta `CitaSMS`.
4. Espera a que Gradle sincronice el proyecto.
5. Conecta el Redmi por USB con depuración USB activada y pulsa Run.

Requisitos de compilación: Android Studio reciente, JDK 17 y Android SDK Platform 35.

## Permisos
Al iniciar, concede acceso a SMS cuando Android lo solicite. Si lo deniegas, la lectura inicial y la detección automática no funcionarán.

## Flujo
1. Al abrir por primera vez, la app analiza el historial disponible.
2. Los SMS nuevos se analizan al llegar.
3. Los candidatos aparecen en la lista.
4. Pulsa **Revisar y añadir** para abrir el formulario de Calendar con título, fecha, hora y lugar precompletados.
5. Comprueba los datos y guarda desde Calendar.

No se ha incluido un APK compilado: este entorno permite entregar el proyecto fuente, pero no dispone de Android SDK/Gradle configurado para compilar y probar un APK real en el dispositivo.


## Compilar sin instalar Android Studio (GitHub Actions)
El proyecto incluye `.github/workflows/build-apk.yml`.
1. Crea un repositorio nuevo en GitHub y sube el contenido de esta carpeta.
2. Abre la pestaña **Actions** del repositorio.
3. Ejecuta **Build CitaSMS APK** (o espera a que termine el primer push).
4. Al terminar, entra en la ejecución y descarga el artefacto **CitaSMS-debug-apk**.
5. Dentro encontrarás `app-debug.apk`. Pásalo al Redmi e instálalo.

La compilación requiere conexión y una cuenta de GitHub, pero no Android Studio en tu ordenador.
