# NeuroZen Mobile (Android)

App Android nativa de **NeuroZen**, plataforma de detección temprana y gestión del estrés laboral. Una sola app con dos experiencias según el rol que devuelve el login:

- **Trabajador (`USER`)**: registro de estrés, test de autoevaluación, síntomas, respiración guiada, pausas activas, carga laboral, desafíos, biblioteca, psicólogos verificados, reservas, informes de progreso, ZenBot y suscripción.
- **Psicólogo (`PSYCHOLOGIST`)**: estado de verificación CPsP, inicio, pacientes, agenda y disponibilidad, informes recibidos y perfil profesional.
- **Administrador (`ADMIN`)**: solo web. En el móvil ve «Usa la versión web para administrar».

Backend: `https://neurozen-backend-mobile.onrender.com/api/v1/` ([Swagger](https://neurozen-backend-mobile.onrender.com/swagger/index.html)).

## Diseño

Se conserva el diseño de la primera versión de la app y se aplica a todas las pantallas nuevas:

- **Paleta original:** Forest 700 `#2A6B4B` (primario), Emerald 600 `#4E9F6E` (secundario), Sage 500, Mint 300, fondo Mint 100 `#F0FAF3`, Sand 100 y superficies blancas. Tema claro, igual que la versión original.
- **Tipografía original** (títulos en negrita/semibold, cuerpo 14–16 sp).
- **Componentes originales:** bienvenida con degradado verde y logo en tarjeta blanca; login/registro con degradado, insignia redondeada, tarjeta blanca de 28 dp y selector *Paciente / Psicólogo*; inicio con cabecera de imagen, frase del día, próxima sesión, «¿Cómo te sientes hoy?», carrusel «Tu equilibrio» y «Recomendado para ti»; cabeceras en degradado («Centro de Ayuda»); «Portal del Psicólogo 🩺» con insignias de estadísticas y tarjetas de cita con avatar; barra inferior con indicador menta (trabajador) y verde esmeralda (psicólogo).
- **Acabado profesional (v2.2):** tarjetas planas con borde fino, cabeceras en degradado con esquinas redondeadas, avatares con iniciales, filas de ajustes agrupadas con íconos, bloques de fecha en las citas, mosaicos de funciones en Bienestar, perfiles completos (estadísticas, progreso, secciones de cuenta, privacidad y ayuda) para trabajador y psicólogo.
- **Ícono de la app:** el logo de NeuroZen (cabeza con árbol neuronal) como ícono adaptativo con fondo crema `#FDF1E1`, ícono temático monocromo (Android 13+) y versiones legacy en todas las densidades.

## Requisitos

- Android Studio (versión compatible con AGP 9.1) y JDK 17.
- Dispositivo o emulador con Android 8.0 (API 26) o superior.

## Compilar y ejecutar

```bash
./gradlew assembleDebug          # APK en app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # pruebas unitarias y de integración (JVM)
./gradlew connectedDebugAndroidTest  # Room + Compose UI (emulador conectado)
```

En Android Studio: *Open* → carpeta del proyecto → *Run 'app'*.

## Configuración

Propiedades opcionales en `gradle.properties` o con `-P`:

| Propiedad | Por defecto | Uso |
|---|---|---|
| `neurozen.apiBaseUrl` | `https://neurozen-backend-mobile.onrender.com/api/v1/` | URL base de la API (debe terminar en `/`) |
| `neurozen.resetLinkHost` | `neurozen-web.onrender.com` | Dominio del enlace de recuperación de contraseña que envía el correo. **Cámbialo al dominio real de la web** y publica `/.well-known/assetlinks.json` para que el App Link se verifique |

Para un backend local usa un túnel HTTPS (por ejemplo ngrok): la app solo permite HTTPS.

**Render gratuito:** el primer request del día puede tardar ~50 s. La app usa timeouts de 60 s y muestra «el servidor puede tardar hasta un minuto en despertar».

## Credenciales de prueba

El repositorio no incluye credenciales. Para probar:

1. **Trabajador:** crea una cuenta con «Busco apoyo».
2. **Psicólogo:** crea una cuenta con «Soy psicólogo» y un N.° de colegiatura. Quedará *En revisión* hasta que un administrador la apruebe desde la **web** (`PATCH /admin/professionals/{id}/verify`).
3. Comparte las cuentas de demo del equipo en el informe, no en el código.

## Arquitectura

MVVM + Clean Architecture en un solo módulo, separado por paquetes:

```
com.example.neurozen_front
├── domain/          Kotlin puro, sin Android (100 % testeable en JVM)
│   ├── model/       Session, Appointment, StressTrigger, Outcome/AppError…
│   ├── logic/       Reglas: AssessmentScoring, AppointmentRules, CardValidator,
│   │                CrisisDetector, ZenBotEngine, StressStatsCalculator,
│   │                RecommendationEngine, ReminderMath, DateTimeUtils, DayCodec
│   ├── repository/  Interfaces
│   └── usecase/     Logout, GetStressStats, ShareReport, BookAppointment
├── data/
│   ├── remote/      NeuroZenApi (Retrofit), DTOs, mappers, ErrorMapper
│   ├── local/       Room: respiración, carga laboral, desafíos, caché de citas
│   ├── session/     DataStore cifrado (Keystore AES-GCM) + AuthInterceptor
│   └── repository/  Implementaciones
├── notifications/   AlarmManager: pausas, ergonomía, citas (1 h antes)
├── di/              Módulos Hilt
└── presentation/
    ├── navigation/  AppRoot: splash → enlace de reset → auth → navegación por rol
    ├── auth/        Bienvenida, login, registro, recuperar contraseña
    ├── worker/      Pantallas y ViewModels del trabajador
    ├── psychologist/Pantallas y ViewModels del psicólogo
    ├── components/  UiState, StateContent (loading/empty/error), campos, botones
    └── theme/       Verde bosque, menta, beige, turquesa, gris piedra (claro/oscuro)
```

Decisiones clave:

- **Contrato:** ids `Long`, fechas en UTC (se convierten a/desde `America/Lima`), enumeraciones en MAYÚSCULAS.
- **Sesión:** el JWT se guarda cifrado; se lee `exp`. Un **401** en una llamada autenticada cierra la sesión; los 400/403/404/409 muestran el `message` del backend sin cerrar sesión.
- **Datos sensibles:** sin logs de token, contraseñas, tarjetas ni registros (logging `BASIC` solo en debug). `FLAG_SECURE` en informes y fichas de pacientes. Backups deshabilitados. Room se borra al cerrar sesión o eliminar la cuenta.
- **Tarjetas:** validación Luhn, vencimiento y CVV en la app; nunca se guardan en el teléfono y el formulario se limpia tras enviar. Es un pago de demostración; para publicar en Google Play con suscripciones a funciones de la app se debe usar Google Play Billing.

## Pruebas

| Tipo | Ubicación | Qué cubre |
|---|---|---|
| Unitarias (JUnit 4) | `src/test/.../domain` | Puntaje del test (ítems invertidos), máquina de estados de citas, Luhn/vencimiento/CVV, ZenBot y crisis, conversión Lima↔UTC, próxima pausa, estadísticas, recomendaciones, validadores, JWT, días de disponibilidad |
| Unitarias (MockK + coroutines-test) | `src/test/.../presentation` | LoginViewModel, RegisterViewModel, ZenBotViewModel, ShareReportUseCase |
| Integración (MockWebServer) | `src/test/.../data` | Auth y citas contra HTTP falso: 200/201, 401, 409, error de red, cuerpo y rutas enviadas, caché |
| Integración Room (instrumentada) | `src/androidTest/RoomIntegrationTest` | Base real en memoria y borrado al cerrar sesión |
| UI (Compose Test) | `src/androidTest/ComposeUiTest` | Login (entrada, envío, error) y ZenBot (mensaje de crisis con líneas de ayuda) |

CI/CD: `.github/workflows/pipeline.yml` tiene cuatro jobs. `quality` (pruebas unitarias, JaCoCo, Android Lint y Quality Gate de SonarQube Cloud) corre en cada push/PR a `develop` y `main`. `smoke` (pruebas instrumentadas en emulador) corre en PRs y pushes a `main`. `build-release` (APK/AAB firmados) y `distribute` (Firebase App Distribution, grupo `qa`) corren solo en push a `main`. `.github/workflows/cd-production-mobile.yml` publica un APK firmado en GitHub Releases al subir un tag `vX.Y.Z`. Detalle en `docs/cambios-cicd.md`.

## Alcance y limitaciones

Implementado solo lo que el backend soporta o lo que puede vivir de forma local y honesta. Quedan **fuera de alcance**, documentados en `docs/AUDITORIA.md`:

- **Análisis biométrico con cámara (US05):** no hay endpoint ni un modelo validado para inferir estrés desde la cámara; mostrar un resultado simulado sería engañoso con datos de salud.
- **Grupos de apoyo (US16):** no hay backend de comunidad ni moderación.
- **Subida de documentos, estado SUSPENDED, reprogramar cita, modalidad virtual/presencial, verificación por correo, facturación del psicólogo, refresh token:** no existen en el contrato actual.
- **Push (FCM):** requiere que el backend registre el token del dispositivo y envíe mensajes. Mientras tanto se usan notificaciones locales.

Supuestos a confirmar con el backend: el formato de los días de `availabilitySchedule` (la app conserva el estilo que devuelve el servidor y usa `MONDAY…` por defecto), los valores de `workType`/`ageRange` y el esquema de `health_metrics`.

## Siguientes pasos sugeridos

1. Extraer los textos a `strings.xml` (preparar inglés).
2. Cifrar Room con SQLCipher si se agregan datos sensibles offline.
3. Endpoints en el backend para FCM, reprogramación y documentos de verificación.
