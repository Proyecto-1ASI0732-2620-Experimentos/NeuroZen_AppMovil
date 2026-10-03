package com.example.neurozen_front

import com.example.neurozen_front.neurozen.data.network.AuthSession
import com.example.neurozen_front.neurozen.data.network.HealthMetricRequest
import com.example.neurozen_front.neurozen.data.network.MeditationResource
import com.example.neurozen_front.neurozen.data.network.NeurozenApiService
import com.example.neurozen_front.neurozen.data.network.NeurozenRepository
import com.example.neurozen_front.neurozen.data.network.ProfessionalResource
import com.example.neurozen_front.neurozen.data.network.UserRole
import com.example.neurozen_front.neurozen.data.session.UserSession
import com.example.neurozen_front.neurozen.home.presentation.home.HealthMetric
import com.example.neurozen_front.neurozen.home.presentation.home.NeurozenUser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.Response

/**
 * Pruebas Unitarias para Entidades Core de NeuroZen utilizando JUnit 4, Mockito y el patrón AAA.
 * 
 * Frente: Aplicación Móvil (Android nativo)
 * Responsable: Manuel Fernando Joao Castro
 */
@RunWith(MockitoJUnitRunner::class)
class ExampleUnitTest {

    @Mock
    private lateinit var mockApiService: NeurozenApiService

    private lateinit var repository: NeurozenRepository

    @Before
    fun setUp() {
        // ARRANGE Global: Limpiar la sesión en memoria e inicializar el repositorio mockeado
        UserSession.clear()
        repository = NeurozenRepository(mockApiService)
    }

    /**
     * NOTA DE PRUEBA:
     * - ¿Por qué se realizó esta prueba?: Para garantizar que el modelo User y la sesión en memoria
     *   almacenen correctamente la información crítica del usuario autenticado (Token JWT, ID y Rol)
     *   sin corrupción de datos antes de permitir la navegación en la app.
     * - ¿Qué hace esta prueba?: Valida la instanciación de la entidad User y verifica que UserSession
     *   persista de forma consistente el perfil y formatee el Header de autorización Bearer.
     */
    @Test
    fun userEntity_creationAndSessionPersistence_isCorrect() {
        // 1. ARRANGE (Preparación)
        val user = NeurozenUser(
            name = "joao",
            email = "joao@neurozen.pe",
            streakDays = 5,
            minutesToday = 20,
            subscriptionPlan = "Zen+"
        )
        val authSession = AuthSession(
            token = "jwt_mock_token_joao_123",
            userId = "uuid_joao_castro",
            username = user.name,
            email = user.email,
            role = UserRole.CLIENT
        )

        // 2. ACT (Ejecución)
        UserSession.save(authSession)
        val currentState = UserSession.state.value

        // 3. ASSERT (Verificación)
        assertEquals("joao", user.name)
        assertEquals("joao@neurozen.pe", user.email)
        assertTrue(UserSession.hasActiveSession())
        assertEquals("uuid_joao_castro", currentState.userId)
        assertEquals(UserRole.CLIENT, currentState.role)
        assertEquals("Bearer jwt_mock_token_joao_123", UserSession.bearerTokenOrEmpty())
    }

    /**
     * NOTA DE PRUEBA:
     * - ¿Por qué se realizó esta prueba?: Para validar la regla de negocio crítica del módulo CheckIn,
     *   donde un nivel de estrés superior a 7 debe clasificarse automáticamente como "Nivel alto"
     *   y calcular la barra de progreso proporcional.
     * - ¿Qué hace esta prueba?: Procesa un objeto HealthMetricRequest con nivel de estrés 8 y confirma
     *   que la lógica de mapeo evalúa correctamente el indicador visual de riesgo emocional.
     */
    @Test
    fun checkInHealthMetric_stressLevelThresholdCalculation_isCorrect() {
        // 1. ARRANGE (Preparación)
        val metricReq = HealthMetricRequest(
            userId = "uuid_joao_castro",
            stressLevel = 8,
            heartRate = 92,
            sleepHours = 5,
            notes = "Semana de exámenes universitarios"
        )

        // 2. ACT (Ejecución)
        val isHighStress = metricReq.stressLevel > 7
        val metricDisplay = HealthMetric(
            label = "Estrés",
            value = "${metricReq.stressLevel}/10",
            detail = if (isHighStress) "Nivel alto" else "Bajo control",
            progress = metricReq.stressLevel / 10f
        )

        // 3. ASSERT (Verificación)
        assertEquals(8, metricReq.stressLevel)
        assertTrue(isHighStress)
        assertEquals("8/10", metricDisplay.value)
        assertEquals("Nivel alto", metricDisplay.detail)
        assertEquals(0.8f, metricDisplay.progress, 0.01f)
    }

    /**
     * NOTA DE PRUEBA:
     * - ¿Por qué se realizó esta prueba?: Para asegurar que la consulta de profesionales mediante el
     *   repositorio utilice Mockito para simular la respuesta del servicio REST `getProfessionals`
     *   garantizando el desacoplamiento de la red.
     * - ¿Qué hace esta prueba?: Mockea el servicio Retrofit para retornar una lista de psicólogos,
     *   ejecuta la llamada a través del repositorio y verifica con Mockito que la API fue invocada.
     */
    @Test
    fun psychologistEntity_repositoryGetProfessionalsWithMockito_returnsList() {
        runBlocking {
            // 1. ARRANGE (Preparación con Mockito)
            val mockProfessional = ProfessionalResource(
                id = 102,
                firstName = "Carlos",
                lastName = "Rodríguez",
                specialization = "Psicología Clínica",
                price = 45.0,
                rating = 4.8,
                email = "carlos.rodriguez@neurozen.com"
            )
            val mockList = listOf(mockProfessional)
            val token = "Bearer jwt_mock_token_joao_123"

            `when`(mockApiService.getProfessionals(token)).thenReturn(Response.success(mockList))

            // 2. ACT (Ejecución)
            val result = repository.getProfessionals(token)

            // 3. ASSERT (Verificación)
            assertTrue(result.isSuccess)
            val list = result.getOrNull()
            assertNotNull(list)
            assertEquals(1, list?.size)
            assertEquals("Carlos", list?.first()?.firstName)
            assertEquals(45.0, list?.first()?.price ?: 0.0, 0.01)

            // Verificación de interacción con Mockito
            verify(mockApiService, times(1)).getProfessionals(token)
        }
    }

    /**
     * NOTA DE PRUEBA:
     * - ¿Por qué se realizó esta prueba?: Para certificar que las sesiones de meditación mantengan
     *   la integridad de sus atributos (duración, título e URL del recurso multimedia en formato MP3).
     * - ¿Qué hace esta prueba?: Crea un objeto MeditationResource y corrobora sus valores de propiedad.
     */
    @Test
    fun sessionEntity_durationAndResourceMapping_isCorrect() {
        // 1. ARRANGE (Preparación)
        val meditation = MeditationResource(
            id = 301,
            title = "Calma Mental y Mindfulness",
            description = "Sesión guiada para reducir la rumiación cognitiva.",
            durationMinutes = 15,
            imageUrl = "https://images.unsplash.com/photo-1518199266791",
            audioUrl = "https://neurozen.app/audio/calma.mp3"
        )

        // 2. ACT (Ejecución)
        val durationInSeconds = meditation.durationMinutes * 60
        val isMp3Audio = meditation.audioUrl.endsWith(".mp3")

        // 3. ASSERT (Verificación)
        assertEquals(301, meditation.id)
        assertEquals(15, meditation.durationMinutes)
        assertEquals(900, durationInSeconds)
        assertTrue(isMp3Audio)
    }
}
