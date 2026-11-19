# Informe: adaptación de la app móvil al backend auditado (DNI, consentimientos, biblioteca con YouTube y planes)

**Alcance:** solo la app Android (Kotlin + Jetpack Compose). **Backend no modificado.** Sin cambios en
`build.gradle.kts`, `libs.versions.toml`, `gradle.properties`, firmas, URL de la API, `AndroidManifest.xml`,
`network_security_config.xml` ni `.github/workflows/`. No se agregaron dependencias.

Equivale a los dos prompts que ya se aplicaron al frontend web: (1) DNI, consentimientos y verificación de psicólogos;
(2) biblioteca con enlaces de YouTube y acceso por planes.

---

## 0. Contrato verificado contra el código real del backend

Se leyó el código de la rama `feature/dni-biblioteca-planes` del repositorio del backend:

| Punto | Backend real | Estado de la app antes | Ahora |
|---|---|---|---|
| Registro | `SignUpResource` con `dni`, `confirmPassword`, `acceptedPrivacyPolicy` | No enviaba ninguno de los tres → **el registro respondía 400** | Los envía |
| Especialidad | Catálogo cerrado: `clinical`, `organizational`, `educational`, `family`, `childAdolescent`, `health`, `social`, `neuropsychology` | **Texto libre** («ej: Terapia Cognitivo Conductual») → **el registro de psicólogos respondía 400** | Lista cerrada con etiquetas en español |
| Colegiatura | `^[0-9]{3,6}$` | Aceptaba de 3 a 8 dígitos, incluidos dígitos de otros alfabetos (`isDigit`) | De 3 a 6 dígitos 0-9 |
| Nombre | `PersonName`: 2 palabras, ≤120, letras + `' ’ - .` | Solo pedía 3 caracteres y un espacio | Mismas reglas y mensajes |
| Contraseña | ≥8, letra y número (`char.IsLetter/IsDigit`), ≤72 bytes | Sin el límite de 72 bytes | Igual al backend |
| `workType` | Solo `formal` o `informal` | Ofrecía «Presencial / Remoto / Híbrido» → **guardar el perfil respondía 400** | `formal` / `informal` |
| Cambio de correo en `PUT /users/{id}` | Exige `currentPassword` | No lo enviaba → **cambiar el correo respondía 400** | Pide la contraseña actual solo si el correo cambia |
| `GET /users/{id}` | Devuelve `dni` (`""` en cuentas antiguas) | No lo leía | DNI en el perfil (solo lectura, enmascarado) |
| 403 de psicólogo no verificado | «Tu cuenta profesional aún no está verificada» en citas (detalle, confirmar, completar) | Mostraba el texto como error genérico | Vuelve a la pantalla de estado de verificación |
| Biblioteca | Lista filtrada por plan; detalle fuera del plan → 403 «Este recurso está disponible desde el plan …» | El 403 salía como error genérico; los enlaces se abrían fuera de la app | Pantalla «Mejorar plan» y reproductor de YouTube dentro de la app |
| Planes | `planId` 1 = Básico, 2 = Avanzado, 3 = Profesional | La app los llamaba «Básico / Premium / Empresarial» | «Básico / Avanzado / Profesional», como el backend y la web |
| Suscripción | Responde `nameUser`, `lastNameUser`, `emailUser`, `cardLast4` | Ya coincidía | Sin cambios |

## 1. Archivos modificados o creados

| Archivo | Motivo |
|---|---|
| `domain/logic/Validators.kt` | Reglas del backend: `dniError`, `fullNameError`, `normalizeFullName`, colegiatura de 3 a 6 dígitos ASCII, `digitsOnly` (descarta lo que no sea 0-9, también al pegar), `maskDni`, contraseña ≤72 bytes y mensajes del backend |
| `domain/logic/Catalogs.kt` | `SPECIALTIES` (catálogo del backend y etiquetas de la web), `WORK_TYPES` (`formal` / `informal`), `AGE_RANGES` y planes con los nombres del backend |
| `domain/logic/YouTube.kt` (nuevo) | Extrae el id de `watch?v=`, `m.youtube.com`, `youtu.be`, `embed` y `shorts`; arma las URL de reproducción y de miniatura solo a partir del id validado |
| `domain/model/Models.kt` | `RegisterData` con `dni`, `confirmPassword` y `acceptedPrivacyPolicy`; `UserProfile.dni`; `UpdateProfileData.currentPassword`. La `Session` (guardada cifrada) **no** tiene DNI |
| `domain/model/Outcome.kt` | `AppError.TooManyRequests` (429), `isProfessionalNotVerified()` e `isPlanRequired()` |
| `domain/repository/Repositories.kt` | Evento `professionalNotVerifiedEvents` |
| `data/remote/Dtos.kt` | `SignUpDto` (`dni`, `confirmPassword`, `acceptedPrivacyPolicy`), `UserDto.dni`, `UpdateUserDto.currentPassword` (Gson no envía los `null`, así que el backend conserva esos valores) |
| `data/remote/Mappers.kt` | `dni` del usuario; miniatura de YouTube cuando el recurso no trae una; autor vacío → sin autor |
| `data/remote/ApiCall.kt` | 429 → «Demasiados intentos…» |
| `data/repository/RemoteRepositories.kt` | El registro envía DNI, confirmación y los dos consentimientos, con el nombre normalizado. El perfil envía `currentPassword` solo cuando cambia el correo |
| `data/session/AuthInterceptor.kt`, `SessionRepositoryImpl.kt` | Detecta el 403 «no está verificada» con `peekBody` (no consume la respuesta) y emite un evento solo para psicólogos |
| `presentation/components/FormFields.kt` (nuevo) | `DigitsField` (teclado numérico, solo 0-9, longitud máxima) y `SpecialtyField` (lista cerrada) |
| `presentation/components/YouTubePlayer.kt` (nuevo) | Reproductor oficial de YouTube (`youtube-nocookie`) dentro de la app. Usa un WebView que solo carga YouTube y bloquea cualquier otra navegación |
| `presentation/auth/AuthViewModels.kt`, `AuthScreens.kt` | Registro con DNI, especialidad cerrada, colegiatura numérica y dos casillas separadas y desmarcadas (se conserva la de datos de salud). Errores 400 y 409 (correo, DNI, colegiatura) bajo su campo, y el error se borra al corregir el campo. 429 como mensaje general. El DNI y las contraseñas solo viven en memoria |
| `presentation/worker/ProfileScreens.kt` | Fila «DNI» de solo lectura, enmascarada (`****5678`, se muestra al tocar) y «No registrado» en cuentas antiguas. `workType` correcto. Contraseña actual al cambiar el correo. Nombre validado solo si cambia, para no bloquear a cuentas antiguas |
| `presentation/worker/LibraryScreens.kt`, `WorkerNavHost.kt` | Lista siempre desde la API, recargada al volver a la pantalla (por ejemplo, después de suscribirse). Banner «Desbloquea toda la biblioteca con el plan Avanzado» (sin plan o Básico). Detalle: reproductor dentro de la app para videos **y** audios, «Abrir en YouTube» como alternativa, descripción completa (con el aviso clínico), y pantalla «Recurso no incluido en tu plan» con «Mejorar plan» ante el 403. Tipo «Ejercicio» (antes se mostraba como «Lectura») |
| `presentation/psychologist/PsychologistRoot.kt` | Ante el 403 de no verificado recarga el perfil y muestra la pantalla de estado. Al cambiar nombre o colegiatura vuelve a leer el perfil. Reenvío tras rechazo con especialidad cerrada, colegiatura numérica y nombre validado |
| `presentation/psychologist/PsychologistScreens.kt` | Especialidad cerrada en el perfil. «Corregir nombre» con aviso previo («tu cuenta volverá a revisión»). El 403 al confirmar o completar ya no muestra un error genérico |
| `presentation/worker/ProfessionalScreens.kt` | Directorio con la etiqueta de la especialidad («Psicología clínica») en lugar de la clave (`clinical`) |
| `presentation/worker/StressScreens.kt` | Pulso: solo dígitos 0-9 |
| Pruebas: `domain/ValidatorsTest.kt`, `presentation/ViewModelTests.kt` | **Actualizadas, no eliminadas:** mensajes de contraseña del backend; el registro ahora exige DNI y privacidad, y la especialidad del catálogo |
| Pruebas nuevas: `domain/BackendContractRulesTest.kt`, `data/SignUpAndProfileContractTest.kt`, `presentation/RegisterViewModelContractTest.kt` | Ver §3 |

## 2. Reproductor de YouTube

No hay una librería de YouTube en el proyecto y no se agregó ninguna dependencia: se usa el **reproductor oficial
insertado** (`youtube-nocookie.com/embed/ID`) dentro de un `WebView` del sistema:
- Carga una página propia con un `iframe`, con una URL base `https`, porque YouTube exige un *referrer* para reproducir.
- Solo permite dominios de YouTube y Google; cualquier otro enlace se bloquea.
- Sin acceso a archivos y se destruye al salir de la pantalla.
- «Abrir en YouTube» abre `watch?v=ID` en la app de YouTube o en el navegador, por si el dueño no permite la inserción.
- No se promete reproducción con la pantalla bloqueada (YouTube no la permite sin Premium).

## 3. Pruebas ejecutadas

Se ejecutaron en este entorno (JDK 21, Android SDK 36.1, Gradle 9.3.1) los mismos pasos del job `quality` del CI:
`testDebugUnitTest createDebugUnitTestCoverageReport lintDebug`, más `assembleDebug` y `compileDebugAndroidTestKotlin`.

| Paso | Antes (código original) | Ahora |
|---|---|---|
| Pruebas unitarias e integración (JVM) | **70/70** | **89/89** |
| Cobertura JaCoCo | — | Se genera |
| Android Lint | — | Sin errores nuevos; los avisos que quedan son de versiones de dependencias y ya existían |
| `assembleDebug` | — | APK generado |
| Compilación de pruebas instrumentadas | — | Compila |
| Pruebas instrumentadas (emulador) y SonarCloud | — | **No ejecutadas:** no hay emulador en este entorno y SonarCloud corre en el CI |

| Prueba | Resultado |
|---|---|
| DNI: vacío, 7, 9, letras, símbolos, espacio interno, dígitos arábigos y espacios en los extremos | Aprobado |
| Filtro de dígitos (pegar texto, longitud máxima) y DNI enmascarado | Aprobado |
| Nombre: tildes, ñ, ü, guion, apóstrofo, cirílico (pasan); una palabra, números, símbolos y >120 (fallan) | Aprobado |
| Colegiatura de 3 a 6 dígitos ASCII; contraseña ≤72 bytes | Aprobado |
| Catálogos (especialidades, `workType`, planes) iguales al backend | Aprobado |
| Ids y URL de YouTube; miniatura de respaldo; el aviso clínico se conserva | Aprobado |
| 403 por plan ≠ 403 de no verificado; 429 | Aprobado |
| Registro (MockWebServer): envía `dni`, `confirmPassword`, `acceptedTerms`, `acceptedPrivacyPolicy` y el nombre normalizado; la sesión no contiene el DNI | Aprobado |
| Registro de psicólogo: especialidad del catálogo y colegiatura numérica | Aprobado |
| `GET /users/{id}` con DNI y con `""` (cuenta antigua) | Aprobado |
| `PUT /users/{id}`: nunca envía `dni`; `currentPassword` solo si se indica; `workType = formal` | Aprobado |
| Interceptor: un 403 «no verificada» avisa una sola vez y la respuesta sigue legible; un 403 por plan no avisa | Aprobado |
| ViewModel de registro: casillas desmarcadas e independientes, DNI y especialidad, 409 de DNI bajo el campo (se borra al editar), 400 mapeado por campo, 429 | Aprobado |
| Las 70 pruebas anteriores (login, citas, Room, reglas, ZenBot…) | Aprobado |

## 4. Pendientes

**Requieren probarse en un teléfono o emulador** (no se pudo aquí):
- La reproducción real de cada video dentro de la app, porque algunos dueños bloquean la inserción. Si pasa, el admin cambia el enlace desde la web (Admin → Recursos).
- Recorrido visual del registro, el perfil y la biblioteca en pantallas pequeñas.

**Backend o producto** (solo documentado):
- El panel de administración sigue siendo solo web (en el móvil el ADMIN ve «Usa la versión web»).
- Los precios de los planes no coinciden entre la app (S/ 29.90 y S/ 99.90) y la web ($5.82 y $17.53). Hay que decidir cuál es el correcto.
- La app no tiene un mecanismo de versión mínima. **Las versiones ya instaladas no podrán registrarse hasta actualizarse** (el backend exige el DNI), y si tienen recursos fijos recibirán 403 al abrir los que no estén en su plan. Si se agrega un endpoint de versión mínima, convendría mostrar «Actualiza NeuroZen para continuar».

**Legal:**
- El resumen legal existente no menciona el DNI y no hay documentos separados de Términos y de Privacidad (se reutilizó el resumen; no se inventó texto legal).
- La casilla de datos de salud ya existía y se conserva; su texto debe validarlo asesoría legal.

---

## 5. Segunda entrega: registro con un solo campo por dato, lecturas y ejercicios, horario del psicólogo y diseño

Contrato verificado en la rama `fix/registro-campos-unicos` del backend. **Backend no modificado.** Sin cambios de
firma, URL de la API, `build.gradle.kts`, CI ni configuración de tiendas.

### 5.1 Cambios

| Archivo | Cambio |
|---|---|
| `data/remote/Dtos.kt`, `data/repository/RemoteRepositories.kt` | El registro envía **solo los 10 campos** del backend: `fullName, dni, email, password, confirmPassword, role, acceptedTerms, acceptedPrivacyPolicy, specialty, licenseNumber` (los dos últimos solo para psicólogo). Ya no se envían `username`, `firstName` ni `lastName`. El inicio de sesión no cambia |
| `domain/logic/ReadingContent.kt` (nuevo) | Convierte el texto plano de `content` en subtítulos, párrafos y listas (`- ` o `1. `). No interpreta HTML: `<b>` o `<script>` se muestran como texto |
| `presentation/components/ReadingText.kt` (nuevo) | Vista de lectura: letra de 17 sp, interlineado de 27 sp, subtítulos en color primario, listas con viñeta o número |
| `presentation/worker/LibraryScreens.kt` | Biblioteca rediseñada: encabezado, banner para mejorar plan, buscador, filtros por tipo con contador, tarjetas grandes para videos y audios (miniatura 16:9 y duración) y tarjetas compactas para lecturas y ejercicios. Detalle: una lectura va directo al texto, sin imagen de relleno; «5 min de lectura» en lecturas y «6 min» en el resto; un ejercicio con video muestra el reproductor encima de los pasos |
| `domain/logic/Availability.kt`, `presentation/psychologist/AvailabilityEditor.kt` (nuevos) | Reglas y editor del horario (días, «Desde» y «Hasta»). Los días se envían **siempre en inglés** (`monday`…`sunday`), aunque estuvieran guardados en español, porque el backend rechaza otro formato |
| `presentation/psychologist/PsychologistScreens.kt` | **Bug del perfil corregido:** `PUT /professionals/me` exige el horario y un psicólogo nuevo no tiene, así que el perfil no se guardaba. Ahora, si falta, el perfil pide «Disponibilidad semanal» y la guarda en el mismo envío. La Agenda usa el mismo editor |
| `presentation/psychologist/PsychologistRoot.kt` | El reenvío tras un rechazo pide el horario si falta. Si alguna acción llegara a enviar el perfil sin horario, se avisa «Primero indica tus días y horario de atención» en lugar de mostrar el 400 |
| `presentation/auth/AuthScreens.kt` | Registro ordenado en secciones (Tus datos, Datos profesionales, Acceso, Consentimientos) y lista en vivo de requisitos de contraseña (8 o más caracteres, letras y números). Las casillas siguen desmarcadas por defecto |
| `presentation/theme/Theme.kt`, `presentation/components/Components.kt` | Escala tipográfica completa y esquinas redondeadas comunes para diálogos, menús y botones; el estado de error usa el mismo estilo que el estado vacío |

### 5.2 Pruebas (resultados reales en este entorno)

Comando equivalente al CI: `testDebugUnitTest createDebugUnitTestCoverageReport lintDebug assembleDebug compileDebugAndroidTestKotlin` → **BUILD SUCCESSFUL**.
**94/94 pruebas unitarias** (70 originales), 0 fallos. Lint sin errores nuevos (solo los ya incluidos en `lint-baseline.xml`). Se genera `app-debug.apk`.

| Prueba | Resultado |
|---|---|
| Registro: el cuerpo tiene exactamente las claves del backend; especialidad y colegiatura solo para psicólogo | Aprobado |
| Lectura: subtítulos, párrafos y listas numeradas y con viñetas | Aprobado |
| Lectura: el HTML queda como texto; saltos de línea CRLF; contenido vacío | Aprobado |
| Horario: valores por defecto L-V 09:00-18:00, validaciones (sin días, formato y fin posterior al inicio) y días en inglés | Aprobado |
| Horario guardado en español se envía en inglés | Aprobado |
| Tipos de recurso y etiquetas de duración («5 min de lectura» y «6 min») | Aprobado |
| Las 88 pruebas anteriores | Aprobado |

**No ejecutado:** no hay emulador en este entorno (sin KVM) y no se pudieron generar capturas, así que el diseño se
revisó solo en el código. Falta recorrerlo en un teléfono real (ver §4).

---

## 6. Apariencia: modo claro, oscuro o según el sistema

**Dónde:** Perfil → «Apariencia» → «Tema» (trabajador y psicólogo). Abre una ventana con tres opciones: «Según el
sistema» (por defecto), «Claro» y «Oscuro». El cambio se aplica al instante en toda la app, incluidas las barras de
estado y de navegación.

| Archivo | Cambio |
|---|---|
| `data/settings/ThemeSettings.kt` (nuevo) | `ThemeMode` (SYSTEM, LIGHT, DARK) y `ThemeSettingsStore`, que guarda la elección en SharedPreferences propias (`neurozen_appearance`). No es un dato sensible; se conserva al cerrar sesión |
| `presentation/components/AppearanceSetting.kt` (nuevo) | Fila «Tema» y ventana de selección con opciones de radio accesibles |
| `MainActivity.kt` | Aplica el tema elegido y ajusta los iconos de las barras del sistema |
| `presentation/theme/Theme.kt` | Esquema oscuro completo (contenedores, bordes y superficies) y `LocalNzDarkTheme` |
| Pantallas y componentes | Colores fijos que no se leían en modo oscuro cambiados por colores del tema: tarjetas de inicio de sesión y registro, degradados de bienvenida e inicio de sesión, encabezados, chips seleccionados, bloque de fecha, aviso de éxito, círculo de respiración y botón con indicador de carga |
| `sonar-project.properties` | `AppearanceSetting.kt` agregado a las exclusiones de cobertura (componente visual, mismo criterio que el resto) |

**Pruebas:** `ThemeModeTest` (3 pruebas): resolución Sistema/Claro/Oscuro, valor guardado desconocido → Sistema, y la
tienda lee y guarda la preferencia. Totales: **105/105**, lint sin advertencias nuevas, `assembleDebug` y
`compileDebugAndroidTestKotlin` correctos. **Falta revisarlo en un teléfono**, sobre todo en modo oscuro.
