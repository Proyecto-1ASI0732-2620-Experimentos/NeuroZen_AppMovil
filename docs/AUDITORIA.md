# Auditoría de la app móvil NeuroZen

Contraste del código recibido (`NeuroZen_AppMovil-main.zip`) con los requisitos de ambos segmentos y con el contrato real del backend (`/swagger/v1/swagger.json`). La columna **Antes** describe el código original; **Ahora** describe la versión 2.0.0 entregada.

## 1. Hallazgos técnicos del código original

| Problema | Impacto | Corrección |
|---|---|---|
| `BASE_URL = http://192.168.0.90:5059` (IP local) | La app no funcionaba fuera de la PC del desarrollador | URL de Render por defecto, configurable con `-Pneurozen.apiBaseUrl` |
| `usesCleartextTraffic="true"` | Permitía HTTP sin cifrar | `false` + `network_security_config` sin texto plano |
| `SignInResponse.id` como `String` (UUID) | Incompatible con ids `Long` del backend | DTOs nuevos 100 % alineados al swagger |
| Rol `CLIENT` | El backend usa `USER / PSYCHOLOGIST / ADMIN` | `Role` con los 3 valores; ADMIN ve «Usa la versión web» |
| Ruta `appointments/appointments/{patientId}` | Ya no existe | `GET /appointments?patientId=` y `?professionalId=` |
| `ProfessionalResource` con `FirstName/LastName/ImageUrl` | Campos inexistentes | `name`, `specialty`, `image`, `verificationStatus` |
| `UserSession` en memoria (`object`) | Se perdía la sesión al cerrar la app; JWT sin cifrar | DataStore + AES-256/GCM con clave en Android Keystore |
| Dos clientes HTTP (5 s y 15 s) | Fallaban con Render dormido | Un solo `OkHttpClient` (Hilt) con 60 s y aviso «despertando el servidor» |
| Sin capa `domain` | ViewModels acoplados a Retrofit | `domain/` (modelos, reglas, interfaces, casos de uso) sin dependencias Android |
| Sin manejo de 401 | Token vencido dejaba la app rota | Interceptor: 401 en llamadas autenticadas → cierre de sesión (nunca en errores de negocio) |
| Errores genéricos | No se mostraba el `message` del backend | `ErrorMapper` (400/401/403/404/409/5xx, sin red, timeout) |
| Restos de plantilla de tienda (`ProductDetail`, carrito) | Código muerto | Eliminados |
| ZenBot Pro en la app del psicólogo | Contradecía el requisito | Eliminado |
| ZenBot sin detección de crisis | Riesgo para el usuario | `CrisisDetector` + Línea 113 opción 5, 105 y 106 |
| `allowBackup="true"` | Datos de salud en copias de seguridad | Backups y transferencia deshabilitados |
| Mockito en lugar de MockK; 1 prueba de Room | No cumplía el criterio de pruebas | 60+ pruebas (ver README) |
| APK de release versionado en el repo | Artefacto binario en el código fuente | Eliminado; el APK lo genera el CD |

## 2. Segmento trabajador

| Requisito | Antes | Ahora | Observaciones |
|---|---|---|---|
| Bienvenida, registro, login | Parcial | Cumple | Registro «Busco apoyo / Soy psicólogo», términos y consentimiento de datos de salud |
| Recuperar contraseña | No cumple | Cumple | `forgot-password` + `reset-password`, deep link `https://<web>/reset-password?token=` y `neurozen://` |
| Verificación por correo | No cumple | Fuera de alcance | El backend no tiene endpoint de verificación de correo |
| Onboarding (datos opcionales + test inicial) | No cumple | Cumple | Tras el registro: edad, ocupación, modalidad → test |
| Home / dashboard | Parcial (datos simulados) | Cumple | Nivel actual, promedio, hora pico, último test, próxima cita, próxima pausa, recomendaciones |
| Test de autoevaluación (US04) | No cumple | Cumple | 10 preguntas 0–4, resultado del servidor, historial |
| Registro de desencadenantes (US12) | Solo POST sin UI | Cumple | Crear (2 toques), historial y borrar |
| Síntomas físicos (US06) | No cumple | Cumple | Vía `health_metrics` (estrés, sueño, pulso, síntomas en `notes`) |
| Recomendaciones (US07) | No cumple | Cumple | Motor local según nivel, categoría principal y horas semanales |
| Respiración guiada (US08) | Parcial | Cumple | 3 ejercicios animados, historial en Room |
| Pausas activas + recordatorios (US09) | No cumple | Cumple | Días, horario y frecuencia; AlarmManager; se reprograman al reiniciar |
| Psicólogos: buscar por especialidad, ficha, reseñas | Parcial | Cumple | Solo verificados (lo filtra el backend) |
| Reservar con horarios reales (US13) | Parcial | Cumple | Asistente de 4 pasos con `available-slots` (hora de Lima → UTC) |
| Cancelar / ver citas (US14) | Parcial | Cumple | Próximas y pasadas; caché offline en Room |
| Informe de progreso compartido (US11, US15) | No cumple | Cumple | Calculado con datos reales; uno por cita PENDING/CONFIRMED |
| Calificar sesión | No cumple | Cumple | Solo citas COMPLETED no calificadas |
| Biblioteca (US17) | Parcial | Cumple | Filtros por tipo y búsqueda; detalle con texto o enlace a audio/video |
| Desafíos antiestrés (US18) | No cumple | Cumple (local) | Desafíos personales con progreso diario en Room |
| Grupos de apoyo (US16) | No cumple | Fuera de alcance | No hay backend de comunidad; requiere moderación |
| Carga laboral (US19) | No cumple | Cumple (local) | Horas por día, total de 7 días, alerta > 48 h |
| Recordatorios de ergonomía (US20) | No cumple | Cumple | Igual que las pausas, con su propio canal |
| Análisis biométrico con cámara (US05) | No cumple | Fuera de alcance | Sin endpoint ni modelo validado; ver README |
| Perfil, idioma, contraseña, eliminar cuenta | Parcial | Cumple | Eliminar cuenta = Ley 29733 + borrado local |
| Suscripciones | Parcial | Cumple | Validación Luhn/vencimiento/CVV; solo últimos 4 dígitos |
| Notificaciones push (FCM) | No cumple | Fuera de alcance | El backend no registra tokens de dispositivo; se usan notificaciones locales |
| ZenBot con detección de crisis | Parcial | Cumple | Local, funciona sin conexión |

## 3. Segmento psicólogo

| Requisito | Antes | Ahora | Observaciones |
|---|---|---|---|
| Registro con colegiatura CPsP y términos | Parcial (sin colegiatura) | Cumple | `licenseNumber`, `specialty`, `acceptedTerms` |
| Estado de verificación | No cumple | Cumple | PENDING / VERIFIED / REJECTED con motivo y reenvío. `SUSPENDED` no existe en el backend |
| No recibir citas sin verificar | No cumple | Cumple | Pantalla de estado bloquea el panel; el backend lo exige |
| Subida de documentos (DNI, título, constancia) | No cumple | Fuera de alcance | No hay endpoint de archivos; el admin valida en el portal del CPsP |
| Bienvenida tras verificación | No cumple | Cumple | Badge, checklist 4 pasos, «Ir a mi panel» (una sola vez) |
| Inicio: badge, progreso, hoy, por confirmar, próximas | Parcial | Cumple | Datos reales de `GET /appointments?professionalId=` |
| Pacientes: lista, búsqueda, ficha, historial, informes | No cumple | Cumple | Derivado de las citas; ficha con `FLAG_SECURE` |
| Agenda semanal; confirmar, cancelar, atendida | Parcial | Cumple | Navegación por semanas y máquina de estados |
| Reprogramar | No cumple | Fuera de alcance | El backend solo admite CONFIRMED/COMPLETED/CANCELLED |
| Modalidad virtual/presencial | No cumple | Fuera de alcance | No existe en el contrato |
| Disponibilidad semanal | No cumple | Cumple | `availabilitySchedule` (días, inicio, fin) |
| Perfil profesional | Parcial | Cumple | Nombre y colegiatura bloqueados (cambiarlos exige reverificación) |
| Informes recibidos (US15) | No cumple | Cumple | Lista con `FLAG_SECURE` |
| Notificaciones | No cumple | Parcial | Recordatorio local 1 h antes de citas confirmadas; avisos de nuevas citas requieren FCM en backend |
| Suscripción / facturación del psicólogo | No cumple | Fuera de alcance | No hay endpoints |
| Sin test, biometría, triggers, comunidad, ZenBot | No cumple (tenía ZenBot Pro) | Cumple | |

## 4. Requisitos técnicos

| Requisito | Estado |
|---|---|
| Kotlin + Jetpack Compose | Cumple |
| MVVM + Clean Architecture (data / domain / presentation) | Cumple |
| Hilt, Retrofit + OkHttp, Navigation Compose | Cumple |
| JWT en almacenamiento seguro | Cumple (DataStore + Keystore; EncryptedSharedPreferences está obsoleto) |
| Refresh token | No aplica: el backend no lo emite; al vencer el JWT se pide login |
| Room (caché y offline básico) | Cumple: citas, respiración, carga laboral, desafíos; se borra al cerrar sesión |
| Estados loading / success / empty / error | Cumple (`UiState` + `StateContent`) |
| Modo claro/oscuro, accesibilidad | Cumple (esquemas propios, descripciones de contenido, live region en respiración) |
| Internacionalización | Parcial: español en el código; extraer a `strings.xml` es el siguiente paso |
| HTTPS, sin logs sensibles | Cumple (logging BASIC solo en debug) |
| Pruebas unitarias, integración, UI, instrumentadas | Cumple |
| CI GitHub Actions | Cumple (`.github/workflows/ci.yml`) |
