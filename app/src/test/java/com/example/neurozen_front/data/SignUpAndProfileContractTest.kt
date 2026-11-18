package com.example.neurozen_front.data

import com.example.neurozen_front.data.remote.NeuroZenApi
import com.example.neurozen_front.data.repository.AuthRepositoryImpl
import com.example.neurozen_front.data.repository.UserRepositoryImpl
import com.example.neurozen_front.data.session.AuthInterceptor
import com.example.neurozen_front.data.session.SessionRepositoryImpl
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.RegisterData
import com.example.neurozen_front.domain.model.Role
import com.example.neurozen_front.domain.model.UpdateProfileData
import com.example.neurozen_front.domain.repository.SessionRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Sign-up, profile and interceptor against a fake HTTP server, with the audited backend contract. */
class SignUpAndProfileContractTest {
    private lateinit var server: MockWebServer
    private lateinit var api: NeuroZenApi
    private val sessions = mockk<SessionRepository>(relaxed = true) {
        every { requireUserId() } returns 15L
        every { session } returns MutableStateFlow(null)
    }

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder().baseUrl(server.url("/api/v1/")).addConverterFactory(GsonConverterFactory.create()).build()
            .create(NeuroZenApi::class.java)
    }

    @After
    fun tearDown() = runCatching { server.shutdown() }.let { }

    @Test
    fun signUp_sendsDniConfirmPasswordAndBothConsents_andSessionHasNoDni() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":15,"fullName":"Lucía Mendoza","email":"lucia@correo.com","role":"USER","token":"a.b.c"}"""))
        val result = AuthRepositoryImpl(api, sessions).signUp(
            RegisterData(
                fullName = "  Lucía   Mendoza ", email = "lucia@correo.com", password = "Secreta123", role = Role.USER,
                acceptedTerms = true, dni = " 12345678 ", confirmPassword = "Secreta123", acceptedPrivacyPolicy = true
            )
        )
        assertTrue(result is Outcome.Success)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"dni\":\"12345678\""))
        assertTrue(body.contains("\"confirmPassword\":\"Secreta123\""))
        assertTrue(body.contains("\"acceptedTerms\":true"))
        assertTrue(body.contains("\"acceptedPrivacyPolicy\":true"))
        assertTrue(body.contains("\"fullName\":\"Lucía Mendoza\""))
        assertFalse(body.contains("specialty"))
        // Exactly the backend fields (one field per piece of data): no username/firstName/lastName
        val keys = com.google.gson.JsonParser.parseString(body).asJsonObject.keySet()
        assertEquals(setOf("fullName", "dni", "email", "password", "confirmPassword", "role", "acceptedTerms", "acceptedPrivacyPolicy"), keys)
        // The Session (saved encrypted in DataStore) has no DNI field at all
        coVerify { sessions.save(match { !it.toString().contains("12345678") }, true) }
    }

    @Test
    fun signUp_psychologist_sendsCatalogSpecialtyAndDigitsLicense() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":16,"fullName":"Elena Ramírez","email":"elena@psico.pe","role":"PSYCHOLOGIST","token":"a.b.c"}"""))
        AuthRepositoryImpl(api, sessions).signUp(
            RegisterData("Elena Ramírez", "elena@psico.pe", "Segura2026", Role.PSYCHOLOGIST, true, "clinical", "45821",
                dni = "87654321", confirmPassword = "Segura2026", acceptedPrivacyPolicy = true)
        )
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"specialty\":\"clinical\""))
        assertTrue(body.contains("\"licenseNumber\":\"45821\""))
        assertTrue(body.contains("\"role\":\"PSYCHOLOGIST\""))
    }

    @Test
    fun getMe_readsDni_andLegacyAccountWithoutDni() = runTest {
        server.enqueue(MockResponse().setBody("""{"id":15,"email":"a@b.pe","fullName":"Ana Torres","role":"USER","dni":"12345678"}"""))
        server.enqueue(MockResponse().setBody("""{"id":15,"email":"a@b.pe","fullName":"Ana Torres","role":"USER","dni":""}"""))
        val repo = UserRepositoryImpl(api, sessions)
        assertEquals("12345678", (repo.getMe() as Outcome.Success).data.dni)
        assertEquals("", (repo.getMe() as Outcome.Success).data.dni)
    }

    @Test
    fun updateMe_neverSendsDni_andSendsCurrentPasswordOnlyWhenGiven() = runTest {
        server.enqueue(MockResponse().setBody("""{"id":15,"email":"nuevo@b.pe","fullName":"Ana Torres","role":"USER"}"""))
        server.enqueue(MockResponse().setBody("""{"id":15,"email":"a@b.pe","fullName":"Ana Torres","role":"USER"}"""))
        val repo = UserRepositoryImpl(api, sessions)
        repo.updateMe(UpdateProfileData("Ana Torres", "nuevo@b.pe", null, null, null, "formal", currentPassword = "Clave2026"))
        val withEmail = server.takeRequest().body.readUtf8()
        assertFalse(withEmail.contains("dni"))
        assertTrue(withEmail.contains("\"currentPassword\":\"Clave2026\""))
        assertTrue(withEmail.contains("\"workType\":\"formal\""))
        repo.updateMe(UpdateProfileData("Ana Torres", "a@b.pe", null, null, null, null))
        assertFalse(server.takeRequest().body.readUtf8().contains("currentPassword"))
    }

    @Test
    fun interceptor_403NotVerified_notifiesOnce_andKeepsBodyReadable() {
        val impl = mockk<SessionRepositoryImpl>(relaxed = true) { every { currentToken() } returns "a.b.c" }
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(impl)).build()
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"Tu cuenta profesional aún no está verificada"}"""))
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"Este recurso está disponible desde el plan Avanzado"}"""))

        val first = client.newCall(Request.Builder().url(server.url("/api/v1/appointments/501")).build()).execute()
        assertTrue(first.body!!.string().contains("no está verificada")) // peekBody did not consume it
        client.newCall(Request.Builder().url(server.url("/api/v1/resource-libraries/3")).build()).execute().close()

        verify(exactly = 1) { impl.notifyProfessionalNotVerified() }
        assertEquals("Bearer a.b.c", server.takeRequest().getHeader("Authorization"))
    }
}
