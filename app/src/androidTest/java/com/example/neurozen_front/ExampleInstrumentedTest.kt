package com.example.neurozen_front

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.neurozen_front.neurozen.data.network.AuthSession
import com.example.neurozen_front.neurozen.data.network.UserRole
import com.example.neurozen_front.neurozen.data.session.UserSession

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Pruebas de Sistema Instrumentadas (End-to-End / System Tests) para NeuroZen en Android.
 * Se ejecutan directamente en un dispositivo Android físico o emulador.
 * 
 * Frente: Aplicación Móvil (Android nativo con Jetpack Compose)
 * Responsable: Joao Castro
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    /**
     * Prueba de Sistema 1: Verificación de Integridad del Paquete y Contexto del Sistema.
     * Valida que la app instancie correctamente el paquete Android com.example.neurozen_front.
     */
    @Test
    fun useAppContext_systemPackageIntegrity_isCorrect() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.neurozen_front", appContext.packageName)
        assertNotNull(appContext.resources)
    }

    /**
     * Prueba de Sistema 2: Simulación de Persistencia de Sesión E2E en Dispositivo.
     * Valida que el estado global de la sesión persista de forma segura en memoria
     * durante la navegación del usuario en el sistema Android.
     */
    @Test
    fun systemE2E_sessionStatePersistence_isSuccessful() {
        // Arrange
        UserSession.clear()
        assertFalse(UserSession.hasActiveSession())

        val mockSession = AuthSession(
            token = "jwt_system_test_e2e_token",
            userId = "uuid_joao_system_test",
            username = "joao_system_user",
            email = "joao.system@neurozen.pe",
            role = UserRole.CLIENT
        )

        // Act
        UserSession.save(mockSession)

        // Assert
        assertTrue(UserSession.hasActiveSession())
        assertEquals("uuid_joao_system_test", UserSession.state.value.userId)
        assertEquals("Bearer jwt_system_test_e2e_token", UserSession.bearerTokenOrEmpty())

        // Clean Up
        UserSession.clear()
        assertFalse(UserSession.hasActiveSession())
    }
}
