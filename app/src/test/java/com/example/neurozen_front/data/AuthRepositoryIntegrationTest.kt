package com.example.neurozen_front.data

import com.example.neurozen_front.data.remote.NeuroZenApi
import com.example.neurozen_front.data.repository.AuthRepositoryImpl
import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.RegisterData
import com.example.neurozen_front.domain.model.Role
import com.example.neurozen_front.domain.repository.SessionRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Repository + Retrofit + Gson against a fake HTTP server (200, 401, 409 and network error). */
class AuthRepositoryIntegrationTest {
    private lateinit var server: MockWebServer
    private lateinit var repo: AuthRepositoryImpl
    private val sessions = mockk<SessionRepository>(relaxed = true)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NeuroZenApi::class.java)
        repo = AuthRepositoryImpl(api, sessions)
    }

    @After
    fun tearDown() = runCatching { server.shutdown() }.let { }

    @Test
    fun signIn_200_returnsSessionWithRoleAndSavesIt() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"id":7,"fullName":"Ana Pérez","email":"ana@empresa.pe","role":"PSYCHOLOGIST","token":"a.b.c"}"""
        ))
        val result = repo.signIn("ana@empresa.pe", "Clave2026")

        assertTrue(result is Outcome.Success)
        val session = (result as Outcome.Success).data
        assertEquals(7L, session.userId)
        assertEquals(Role.PSYCHOLOGIST, session.role)
        coVerify { sessions.save(match { it.userId == 7L && it.token == "a.b.c" }, false) }

        val request = server.takeRequest()
        assertEquals("/api/v1/authentication/sign-in", request.path)
        assertTrue(request.body.readUtf8().contains("\"username\":\"ana@empresa.pe\""))
    }

    @Test
    fun signIn_401_returnsBackendMessage_andDoesNotSave() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Correo o contraseña incorrectos"}"""))
        val result = repo.signIn("ana@empresa.pe", "mala")

        assertTrue(result is Outcome.Failure)
        val error = (result as Outcome.Failure).error
        assertTrue(error is AppError.Unauthorized)
        assertEquals("Correo o contraseña incorrectos", error.message)
        coVerify(exactly = 0) { sessions.save(any(), any()) }
    }

    @Test
    fun signUp_409_licenseAlreadyRegistered() = runTest {
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"message":"La colegiatura ya está registrada"}"""))
        val result = repo.signUp(RegisterData("Luis Rojas", "luis@psi.pe", "Clave2026", Role.PSYCHOLOGIST, true, "Clínica", "12345"))

        assertTrue((result as Outcome.Failure).error is AppError.Conflict)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"role\":\"PSYCHOLOGIST\""))
        assertTrue(body.contains("\"licenseNumber\":\"12345\""))
        assertTrue(body.contains("\"acceptedTerms\":true"))
    }

    @Test
    fun signUp_user_marksNewAccountForOnboarding() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody(
            """{"id":9,"fullName":"Rosa Díaz","email":"rosa@x.pe","role":"USER","token":"t.t.t"}"""
        ))
        repo.signUp(RegisterData("Rosa Díaz", "rosa@x.pe", "Clave2026", Role.USER, true))
        coVerify { sessions.save(match { it.role == Role.USER }, true) }
        assertTrue(!server.takeRequest().body.readUtf8().contains("licenseNumber\":\""))
    }

    @Test
    fun networkError_returnsNetworkFailure() = runTest {
        server.shutdown()
        val result = repo.signIn("ana@empresa.pe", "Clave2026")
        assertTrue((result as Outcome.Failure).error is AppError.Network)
    }
}
