# Registro de cambios CI/CD - NeuroZen App Móvil

Cambios para adaptar el proyecto al pipeline CI/CD (equivalente Android del Lab CI/CD BBVA).

## Fase 1 - Preparación
- `.gitignore`: excluye APK/AAB, keystores, `keystore.properties`, `local.properties` y JSON de cuentas de servicio.
- `AndroidManifest.xml`: verificado HTTPS (`usesCleartextTraffic=false` + `network_security_config` sin cleartext).
- `app-debug.apk` eliminado del repositorio.

## Fase 2 - Cobertura y lint
- `app/build.gradle.kts`: cobertura JaCoCo en `debug` (unit y androidTest) y bloque `lint` con reportes XML/HTML y `abortOnError = true`.
- `app/lint-baseline.xml`: baseline con la deuda previa (3 avisos en `AppointmentsScreen.kt`).
- Rutas de reportes:
    - Cobertura XML: `app/build/reports/coverage/test/debug/report.xml`
    - Lint XML: `app/build/reports/lint-results-debug.xml`
    - JUnit: `app/build/test-results/testDebugUnitTest/`

## Fase 3 - Firma, versión y R8
- Firma `release` por variables de entorno (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Solo se aplica si existe `KEYSTORE_PATH`; sin ella `assembleRelease` genera un APK sin firmar en vez de fallar.
- `versionCode = 4 + GITHUB_RUN_NUMBER` y `versionName = 2.2.<run>`: cada build del pipeline es mayor que el último release manual (versionCode 4), así los testers pueden actualizar.
- `isMinifyEnabled = true` e `isShrinkResources = true` en `release`.
- `proguard-rules.pro`: mantiene DTOs de `data.remote` y modelos de `domain.model` (Gson, incluido `Session` guardado en DataStore), más reglas de Gson, Retrofit con `suspend` y stack traces legibles.

## Fase 4 - SonarQube Cloud
- `sonar-project.properties`: fuentes, tests, JUnit, cobertura JaCoCo y Android Lint (`sonar.androidLint.reportPaths`), exclusiones de cobertura para UI/DI/notificaciones y `sonar.qualitygate.wait=true`.
- `sonar.organization` y `sonar.projectKey` NO van en el archivo: el workflow los pasa desde las variables `SONAR_ORG` y `SONAR_PROJECT_KEY`.

## Fase 5 - Workflows
- `.github/workflows/pipeline.yml` (nuevo, reemplaza `android.yml`, `ci.yml` y `cd-staging-mobile.yml`, que hacían trabajo duplicado):
    - `quality`: push/PR a `develop` y `main`.
    - `smoke`: PR y push a `main`, y ejecución manual (emulador API 34).
    - `build-release`: solo push a `main`, tras `quality` y `smoke`; firma, verifica con `apksigner` y calcula SHA-256.
    - `distribute`: Firebase App Distribution, environment `qa-distribution`, grupo `qa`.
- `.github/workflows/cd-production-mobile.yml`: con un tag `vX.Y.Z` publica un APK **release firmado** en GitHub Releases (antes publicaba el APK debug).
- JDK 17 en todos los jobs (el mismo que usaba el CI original del equipo).

## Fase 6 - Pruebas
- `OutcomeTest.kt`: pruebas de la clase `Outcome`.
- Para el experimento E1 (regresión controlada) se recomienda usar una regla con pruebas existentes, por ejemplo `AssessmentScoring` (`AssessmentScoringTest.kt`) o `StressStatsCalculator` (`StatsAndRecommendationsTest.kt`).

## Secrets y variables requeridos (Settings > Secrets and variables > Actions)

| Tipo | Nombre | Uso |
| --- | --- | --- |
| Secret | `SONAR_TOKEN` | quality |
| Variable | `SONAR_ORG` | quality (`proyecto-1asi0732-2620-experimentos`) |
| Variable | `SONAR_PROJECT_KEY` | quality (`Proyecto-1ASI0732-2620-Experimentos_NeuroZen_AppMovil`) |
| Secret | `KEYSTORE_BASE64` | build-release, cd-production |
| Secret | `KEYSTORE_PASSWORD` | build-release, cd-production |
| Secret | `KEY_ALIAS` | build-release, cd-production |
| Secret | `KEY_PASSWORD` | build-release, cd-production |
| Variable | `FIREBASE_APP_ID` | distribute |
| Secret | `FIREBASE_SERVICE_ACCOUNT` | distribute |

Environment requerido: `qa-distribution`. `SONAR_HOST_URL` no se usa (el host va fijo en el workflow).
