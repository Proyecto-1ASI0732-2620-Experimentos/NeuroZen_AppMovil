# Informe de corrección y mejora — App móvil NeuroZen (Android)

Fecha: 2026-11-27. Base: `NeuroZen_AppMovil-main` (zip entregado, **sin `.git`**).

## 1. Revisión previa (dónde estaba cada problema)

| Id | Ubicación encontrada en el código original | Diagnóstico |
|---|---|---|
| A1 | `presentation/worker/HomeScreen.kt` — `HomeContent`: el carrusel «Tu equilibrio» era el 5.º bloque, después de la frase del día, la próxima cita y el selector de ánimo; cabecera de 230 dp. `HomeViewModel` vivía en el mismo archivo. | No había gráfico de evolución: el resumen era el carrusel. Sin datos mostraba «0/10» y «—». Confirmado. |
| A2 | `WellnessScreens.kt` — `BreathingScreen`: solo animación de escala y texto. Sin `VIBRATE` en el manifiesto. | Confirmado. |
| A3 | El App Link **ya existía** (`AndroidManifest.xml`, `intent-filter autoVerify` + `MainActivity.extractResetToken` + `onNewIntent`), pero: host por defecto `neurozen-web.onrender.com` (no la web real), no se validaba el host ni el token (`path.contains("reset-password")`), y el campo manual pedía copiar solo el «token». | Parcialmente resuelto; se completó. |
| M1 | `Components.kt` — `Pill`/`StatusPill`: texto del mismo color que el fondo al 15 % (p. ej. ámbar `#E0A030` sobre su tinte ≈ 2:1). Sin icono. Estrellas y «0.0» sin descripción accesible. | Confirmado. |
| M2 | Test `x/40` (`StressScreens.kt`), registros `x/10`, Inicio `x/10` y `x/40`. | Confirmado. |
| M3 | `domain/logic/Catalogs.kt` (`PLANS`, `pricePen`) y `ProfileScreens.kt:525` (`S/ %.2f/mes`). | Distinto de la web (tabla §5). |
| B1 | `ProfessionalScreens.kt`: tarifa `null` → «Consultar» o se ocultaba; `rating` 0 → «★ 0.0». Especialidad traducida y avatar por defecto **ya estaban bien** (`Catalogs.specialtyLabel`, `Avatar` con iniciales, imagen `""` → `null` en `Mappers.kt`). | Parcial; corregido lo que faltaba. |
| B2 | `BookingScreen`: lista fija de 21 días (desde mañana), sin `/appointments/availability`, sin próxima fecha, sin cambiar fecha/hora desde el resumen, sin revalidar. El 409 ya conservaba los datos. | Corregido. |
| B3 | `AuthScreens.kt` — checklist con 2 reglas (sin «máx. 72 bytes»), solo en registro. `Validators.passwordError` **ya coincidía** con `PasswordPolicy.cs`. | Corregido. |
| B4 | Sin interpolaciones `null` visibles. `ErrorMapper` ignoraba el `message` del 429 y aceptaba un `message` literal `"null"`. | Corregido. |
| B5 | «Reservar sesión», «Próxima sesión», «Calificar sesión», «Tus sesiones…», notificación «Tu sesión es…». | Corregido. |
| B6 | `PsychologistRoot.kt` — «Suele tomar 1 a 2 días hábiles. Vuelve a abrir la app…». | Corregido. |
| Z1 | ZenBot: saludo sí; sin opciones iniciales, sin recursos reales, sin indicador de escritura, sin anuncio accesible. | Corregido (sin tocar crisis). |

## 2. Tabla de correcciones

| Problema | Archivos modificados | Qué se cambió | Estado |
|---|---|---|---|
| **A1** Resumen abajo | `HomeScreen.kt`, **nuevo** `HomeViewModel.kt`, **nuevo** `domain/logic/StressSummary.kt`, `strings.xml` | Tarjeta «Tu nivel de estrés» justo bajo el saludo (cabecera reducida a 160 dp; cabe sin scroll en 360×640 dp: 160 + 20 + ≈ 300 dp de tarjeta < 536 dp útiles). Índice 0-100, nivel, origen y fecha, detalle «Test PSS-10: 26/40 · Registro: 7/10», mini-gráfico de evolución (últimas 10 mediciones reales) y accesos «Hacer el test»/«Registrar». Sin datos: estado de bienvenida con «Hacer el test» y «Registrar cómo me siento». Carga / error con «Reintentar» / sin datos son estados distintos. El ViewModel pasó a su propio archivo (antes quedaba fuera de la cobertura por la exclusión `*Screen*.kt`). | Resuelto |
| **A2** Respiración solo visual | **nuevos** `domain/logic/BreathingFeedback.kt`, `data/feedback/AndroidFeedbackOutput.kt`, `data/settings/BreathingFeedbackSettings.kt`; `WellnessScreens.kt`, `Catalogs.kt`, `AndroidManifest.xml` | Patrones: inhalar 1 pulso (120 ms), retener 2 pulsos cortos, exhalar 1 pulso largo (450 ms), con `VibrationEffect.createWaveform`; `VibratorManager` en API 31+ y `Vibrator` antes (minSdk 26, así que `createWaveform` siempre existe). Tono suave con `ToneGenerator` en `STREAM_NOTIFICATION`. Silencio → nada; vibración → solo vibra; además respeta los interruptores. Interruptores «Vibración»/«Sonido» (fila completa táctil ≥ 56 dp, rol Switch para TalkBack), persistidos en SharedPreferences (igual que la preferencia de apariencia). Se libera todo al detener, pausar (`ON_PAUSE`), terminar o salir (`finally` + `DisposableEffect`). Fases tipadas en el catálogo. Lógica en clase pura con pruebas. `VIBRATE` declarado. | Resuelto |
| **A3** Token manual | `MainActivity.kt`, `AppRoot.kt`, `AuthScreens.kt`, `AuthViewModels.kt`, **nuevo** `domain/logic/ResetLink.kt`, `app/build.gradle.kts` (solo el valor por defecto de `resetLinkHost` y un `buildConfigField`), `README.md` | App Link sobre `https://neurozen-web-frontend.vercel.app/reset-password?token=…` (host configurable con `-Pneurozen.resetLinkHost`). Se valida esquema, host exacto, ruta `/reset-password` y token (16-512 caracteres ASCII `A-Z a-z 0-9 - _ . ~ + / =`; sin regex). Enlace con token inválido → pantalla con aviso «El enlace no es válido…». App ya abierta → `onNewIntent`. Campo manual: ahora acepta **pegar el enlace completo** del correo. El token no se registra en logs (no hay `Log` en la app). Checklist de contraseña también aquí. | Resuelto en la app · **Pendiente de web** (`assetlinks.json`) y **confirmar backend** (formato del enlace) |
| **M1** Bajo contraste | `Components.kt` (`StatusPill`, `Pill`), **nuevo** `presentation/components/UiMappings.kt`, `ProfessionalScreens.kt`, `DesignSystem.kt`, `Theme.kt` | Etiqueta de cita: texto oscuro sobre fondo claro (claro y oscuro, todos ≥ 4,5:1 verificado por prueba; CONFIRMED usa `#2D5A4A`), icono por estado (reloj, check, X, doble check, ayuda), descripción «Estado: Confirmada». `Pill` genérico: texto `onSurface` + punto de color. Valoración: una sola descripción «Valoración 4,5 de 5, 12 reseñas», estrellas sin descripción propia; sin reseñas «Sin valoraciones aún». Estrellas de reseñas con color de texto accesible. | Resuelto |
| **M2** Escalas distintas | **nuevo** `domain/logic/StressScale.kt`; `HomeScreen.kt`, `StressScreens.kt`, `PsychologistScreens.kt` | Índice 0-100 (PSS `round(p/40×100)`, registro `nivel×10`) con «/100» siempre y el valor original como detalle («Test PSS-10: 26/40», «Registro: 7/10»). Textos de que el test es orientativo y el registro es percepción personal. No cambia datos, envíos ni el cálculo del PSS-10. | Resuelto |
| **M3** Precios | `Models.kt`, `Catalogs.kt`, `ProfileScreens.kt`, `UiMappings.kt`, `strings.xml` | Precios en un solo lugar (`plan_price_*` en `strings.xml`) con los **mismos valores que la web**. Se eliminó `pricePen` del catálogo. | Parcial · **Pendiente de backend** (endpoint de precios) |
| **B1** Datos de relleno | `ProfessionalScreens.kt`, **nuevo** `ProfessionalFormat.kt` | `null`/≤0 → «Tarifa por definir»; rating 0 o 0 reseñas → «Sin valoraciones aún». Especialidad y avatar ya estaban bien. | Resuelto |
| **B2** Reserva | **nuevos** `BookingViewModel.kt`, `domain/logic/BookingCalendar.kt`; `ProfessionalScreens.kt`, `NeuroZenApi.kt`, `Dtos.kt`, `Mappers.kt`, `Models.kt`, `Repositories.kt`, `RemoteRepositories.kt`, `DateTimeUtils.kt` | Calendario de 30 días (desde hoy) con `GET /appointments/availability` y `durationMinutes` del tipo; días sin horas deshabilitados y anunciados («…, sin horarios libres»). Si falla: `availabilitySchedule` del profesional (se añadió al DTO); si también falla, todos los días y decide `available-slots`. «Próxima fecha disponible» con botón. «Horarios en hora de Lima (UTC−5)» visible. Desde el resumen: «Cambiar» tipo, fecha u hora sin perder lo demás. Antes de confirmar se revalida la hora; 409 → vuelve a horas con aviso, conservando tipo, fecha y nota. `available-slots` envía `durationMinutes`. | Resuelto |
| **B3** Contraseña | `Validators.kt`, `FormFields.kt` (`PasswordRequirements` compartido), `AuthScreens.kt`, `ProfileScreens.kt` | Checklist en tiempo real con exactamente: ≥ 8 caracteres UTF-16, una letra Unicode, un dígito Unicode, ≤ 72 bytes UTF-8. En registro, nueva contraseña (reset) y cambio de contraseña. | Resuelto |
| **B4** Textos técnicos | `ApiCall.kt` | `message` vacío, `"null"` o `"undefined"` → texto propio; el 429 muestra el `message` del backend. Revisadas las interpolaciones nulas (ninguna visible). | Resuelto |
| **B5** Terminología | `HomeScreen.kt`, `AppointmentsScreen.kt`, `ProfessionalScreens.kt`, `PsychologistScreens.kt`, `ReminderScheduler.kt`, `ReminderSettingsStore.kt`, `Mappers.kt`, `UseCases.kt`, `ZenBotEngine.kt` (texto no relacionado con crisis) | «Cita» en todos los textos de reserva/agenda. Se mantienen «Cerrar sesión» y «Tarifa por sesión» (no son citas). | Resuelto |
| **B6** Verificación | `PsychologistRoot.kt`, `strings.xml` | Texto exacto pedido. Versión en inglés: *"Verification usually takes 1 to 2 business days. We will notify you by email once your account has been reviewed."* (no se creó `values-en` porque no existe; ver §8). | Resuelto (ES) |
| **Z1** ZenBot | **nuevos** `ZenBotViewModel.kt`, `domain/logic/ZenBotResources.kt`; `ZenBotScreen.kt`, `ZenBotEngine.kt` (solo reglas nuevas no relacionadas con crisis), `WorkerNavHost.kt`, API/repositorio de búsqueda | 10 opciones iniciales (se envían como texto, así pasan por la misma detección de crisis). Accesos a pantallas reales (+ «Ver mis citas»). Recursos reales con `GET /resource-libraries/search` (si falla, la lista filtrada por plan y filtrado local); tarjeta con miniatura, título, categoría, duración y «Ver video»/«Ver recurso»; sin coincidencias lo dice. Indicador «ZenBot está escribiendo…». Último mensaje del bot en región viva (TalkBack). La respuesta de crisis se calcula de forma síncrona antes de cualquier búsqueda y **nunca** dispara búsqueda. | Resuelto |
| Extra 1: host del App Link incorrecto | `app/build.gradle.kts`, `README.md` | `neurozen-web.onrender.com` → `neurozen-web-frontend.vercel.app`. | Resuelto |
| Extra 2: `Uri.getQueryParameter` puede lanzar con URIs opacas | `MainActivity.kt` | Protegido con `runCatching`. | Resuelto |
| Extra 3: `session!!` en `AppRoot` | `AppRoot.kt` | Reemplazado por `when (session?.role)`. | Resuelto |
| Extra 4: estrellas ámbar como texto (≈2:1) | `ProfessionalScreens.kt`, `Theme.kt` | Color `StarText` `#8A5A00` en tema claro. | Resuelto |

**Regla 5 respetada:** `CrisisDetector.kt` sin cambios (verificado con `diff`); `CRISIS_MESSAGE`, la condición de crisis de `ZenBotEngine.reply`, los botones «Llamar 113/106» y sus `tel:` no se tocaron. `GET /emergency-resources` **no** se usa; convendría usarlo más adelante para mostrar `verifiedAt` cuando el equipo lo configure, sin reemplazar las líneas actuales.

## 3. Informe de pruebas

Entorno local: JDK 21 (toolchain del proyecto), Android SDK 36/36.1, build-tools 36.0.0, mismas versiones de AGP 9.1.1 / Kotlin 2.3.21 / Gradle 9.3.1 (wrapper sin cambios). `ANDROID_HOME` por variable de entorno (no se creó `local.properties`).

Comandos (los del job `quality` del pipeline + build):

```
./gradlew testDebugUnitTest createDebugUnitTestCoverageReport lintDebug
./gradlew build
./gradlew assembleDebugAndroidTest   # solo compila las instrumentadas
```

| Verificación | Antes (original) | Después |
|---|---|---|
| Pruebas unitarias (JVM) | 105, 0 fallos | **188, 0 fallos** |
| `lintDebug` | OK (0 errores nuevos; 21 avisos de versiones de dependencias, que existían antes) | OK — **0 incidencias nuevas** |
| `./gradlew build` | OK | OK (`BUILD SUCCESSFUL`; los mismos 21 avisos de versiones de dependencias que en el original) |
| Instrumentadas (`connectedDebugAndroidTest`) | No ejecutables aquí | **No ejecutadas**: el contenedor no tiene `/dev/kvm`, así que no hay emulador. Se actualizaron y compilan (`assembleDebugAndroidTest`). Se ejecutarán en el job `smoke` del pipeline. |
| Cobertura del código nuevo/modificado (líneas, con las exclusiones de `sonar-project.properties`) | — | **97,0 %** (482/497) |

Pruebas actualizadas (no eliminadas): `ViewModelTests.zenBot_crisisMessageIsFlagged` y `ComposeUiTest.zenBot_crisisMessage_showsHelplines` construían `ZenBotViewModel()`; ahora reciben la biblioteca (necesaria para las tarjetas de recursos). Siguen comprobando lo mismo y además que una crisis no consulta la biblioteca.

Pruebas nuevas: `StressScaleTest`, `StressSummaryBuilderTest`, `BreathingFeedbackTest`, `BreathingFeedbackAndroidTest` (Vibrator/ToneGenerator/AudioManager/SharedPreferences simulados), `ResetLinkTest`, `AppointmentStatusStyleTest` (contraste ≥ 4,5 en ambos temas), `ProfessionalFormatTest`, `BookingCalendarTest`, `PasswordChecklistTest`, `ZenBotResourcesTest`, `NewEndpointsContractTest` (MockWebServer), `ErrorMessageHardeningTest`, `HomeViewModelTest`, `BookingViewModelTest`, `ZenBotViewModelTest`, `PasswordResetViewModelTest`; instrumentadas `zenBot_showsWelcomeAndQuickOptions` y `statusPill_readsStateForTalkBack`. Todas con `runTest` + `StandardTestDispatcher`, sin `Thread.sleep`.

No se ha ejecutado GitHub Actions ni SonarCloud: los resultados anteriores son locales.

## 4. Dependencias externas

### 4.1 A3 — Frontend web: `public/.well-known/assetlinks.json`

Plantilla en `docs/assetlinks.json.ejemplo`. `package_name`: **`com.example.neurozen_front`**.

Los SHA-256 **no pueden obtenerse desde el código**: el de debug depende del `~/.android/debug.keystore` de cada máquina y el de release está en los secretos del CI. Cómo obtenerlos:
- Release: el job `build-release` ya imprime el certificado (`apksigner verify --print-certs`, línea «SHA-256 digest»). En local: `keytool -list -v -keystore neurozen-release.jks -alias <alias>`. Si se publica en Google Play con App Signing, usar además el SHA-256 de «App signing key» de Play Console.
- Debug: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`, o `./gradlew signingReport`.

El archivo debe servirse en `https://neurozen-web-frontend.vercel.app/.well-known/assetlinks.json` con `Content-Type: application/json` y sin redirecciones. Si la app no está instalada, el mismo enlace abre la web (ya funciona).

### 4.2 A3 — Backend

Confirmar que el correo de `POST /authentication/forgot-password` enlaza exactamente a `https://neurozen-web-frontend.vercel.app/reset-password?token=<token>` con el token codificado para URL (`Uri.EscapeDataString`). Si hoy enlaza a otro dominio o ruta, ese es el cambio exacto. Si el dominio de la web cambia, compilar la app con `-Pneurozen.resetLinkHost=<dominio>`.

La alternativa del código de 6 dígitos **no fue necesaria**. Si se quisiera, sería: `forgot-password { email }` genera un código numérico de 6 dígitos guardado con hash, que vence a los 15 minutos y admite máximo 5 intentos; `reset-password { email, code, newPassword }`, manteniendo el flujo con `token`; 400 si el código es inválido o venció y 429 si se superan los intentos.

### 4.3 M3 — Precios (pendiente de backend)

`GET /subscriptions/plans` → `[ { "planId": 1, "name": "Básico", "price": 0, "currency": "USD" }, … ]` (o `"PEN"` si se decide soles). Mientras no exista, los valores viven solo en `strings.xml` (`plan_price_basic/advanced/professional`).

### 4.4 Otros

No se usó ningún endpoint fuera de la sección 3. Los nuevos que la app consume ya existen: `GET /appointments/availability`, `GET /resource-libraries/search`, `durationMinutes` en `available-slots` y `availabilitySchedule` en `GET /professionals/{id}`.

## 5. M3 — Comparación de precios

| Plan | Precio en la app (antes) | Precio en la web | Ubicación en la app (antes) | Después |
|---|---|---|---|---|
| Básico (1) | «Gratis» | «Gratis» | `Catalogs.kt` `PLANS` (0.0) + `ProfileScreens.kt:525` | `strings.xml` `plan_price_basic` = «Gratis» |
| Avanzado (2) | «S/ 29.90/mes» | «$5.82» | `Catalogs.kt` `PLANS` (29.90) | `plan_price_advanced` = «$5.82» |
| Profesional (3) | «S/ 99.90/mes» | «$17.53» | `Catalogs.kt` `PLANS` (99.90) | `plan_price_professional` = «$17.53» |

No se hicieron conversiones. Se quitó el sufijo «/mes» porque no consta que la web lo muestre; **confirmar con el equipo** si los precios son mensuales. Si se decide mostrar soles (S/), hay que cambiar **a la vez** `public/es.json` y `public/en.json` en la web y `plan_price_*` en la app.

## 6. Infraestructura protegida

Comparados byte a byte con el original y **sin cambios**: `.github/workflows/pipeline.yml`, `.github/workflows/cd-production-mobile.yml`, `sonar-project.properties`, `gradle.properties`, `gradle/wrapper/*`, `gradlew`, `gradlew.bat`, `gradle/libs.versions.toml`, `gradle/gradle-daemon-jvm.properties`, `settings.gradle.kts`, `build.gradle.kts`, `app/lint-baseline.xml`, `app/proguard-rules.pro`, `.gitignore`. No hay `Dockerfile`, `docker-compose`, `google-services.json`, `local.properties` ni keystores. No se añadieron dependencias. En `app/build.gradle.kts` solo cambió el valor por defecto de `resetLinkHost` y se añadió `buildConfigField("RESET_LINK_HOST")`; `signingConfigs`, `buildTypes`, `lint { }` y la cobertura quedaron igual.

Observaciones documentadas, **sin aplicar**:
1. `pipeline.yml`, job `quality`: el análisis de SonarCloud solo corre en `main` (`if: github.ref == 'refs/heads/main'`). Los PR a `develop`/`main` no muestran el Quality Gate hasta después del merge. Impacto: los problemas de código nuevo se detectan tarde. Solución: permitir también `pull_request`, con el token disponible.
2. `sonar-project.properties`, `sonar.coverage.exclusions`: `**/presentation/**/*Screen*.kt` excluye también los ViewModels que están dentro de esos archivos (p. ej. `TriggersViewModel`, `AssessmentViewModel`, `MyAppointmentsViewModel`). Impacto: no se mide su cobertura. Solución: mover los ViewModels a archivos propios, como se hizo aquí con Home, Booking y ZenBot. No se tocó la configuración.
3. `app/lint-baseline.xml`: Lint informa de 25 entradas que ya no aparecen (`LintBaselineFixed`). Impacto: solo informativo. Solución: regenerar la línea base cuando el equipo lo decida.

## 7. Commits

El zip **no trae `.git`**, así que no se hizo ningún commit. Commits recomendados (Conventional Commits), en este orden:

1. `feat(home): show stress summary below greeting with welcome empty state`
2. `feat(stress): add unified 0-100 stress index for display`
3. `feat(breathing): add haptic and sound cues on phase change with persisted toggles`
4. `fix(auth): validate reset link host and token, accept pasted e-mail link`
5. `fix(a11y): accessible appointment status chips and rating descriptions`
6. `refactor(subscriptions): move plan prices to strings.xml aligned with web`
7. `fix(professionals): show «Tarifa por definir» and «Sin valoraciones aún»`
8. `feat(booking): availability calendar, next available date and slot revalidation`
9. `feat(auth): live password checklist matching backend policy`
10. `fix(errors): never show null/undefined and keep backend 429 message`
11. `chore(copy): use «Cita» consistently and update psychologist verification notice`
12. `feat(zenbot): quick options, real library recommendations and typing indicator`
13. `test: cover new logic and update ZenBot tests for injected library`

## 8. Notas

- `res/values-en/strings.xml` no existe en el proyecto, así que no se creó: un `values-en` parcial haría fallar Lint (`MissingTranslation`). Todo texto nuevo está en `res/values/strings.xml` (con `tools:locale="es"`). El resto de pantallas que no se tocaron conserva sus textos en código, como estaba.
- El mini-gráfico de Inicio usa `Canvas` de Compose, sin librerías nuevas.
