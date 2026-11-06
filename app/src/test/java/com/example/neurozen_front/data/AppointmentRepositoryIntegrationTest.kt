package com.example.neurozen_front.data

import com.example.neurozen_front.data.local.AppointmentCacheDao
import com.example.neurozen_front.data.remote.NeuroZenApi
import com.example.neurozen_front.data.repository.AppointmentRepositoryImpl
import com.example.neurozen_front.data.repository.StressRepositoryImpl
import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.AppointmentStatus
import com.example.neurozen_front.domain.model.BookingRequest
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.repository.SessionRepository
import io.mockk.coVerify
import io.mockk.every
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
import java.time.Instant
import java.time.LocalDate

class AppointmentRepositoryIntegrationTest {
    private lateinit var server: MockWebServer
    private lateinit var api: NeuroZenApi
    private val sessions = mockk<SessionRepository> { every { requireUserId() } returns 5L }
    private val cache = mockk<AppointmentCacheDao>(relaxed = true)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder().baseUrl(server.url("/api/v1/")).addConverterFactory(GsonConverterFactory.create()).build()
            .create(NeuroZenApi::class.java)
    }

    @After
    fun tearDown() = runCatching { server.shutdown() }.let { }

    @Test
    fun book_sendsLimaSlotAsUtc_andCallerAsPatient() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody(
            """{"id":1,"patientId":5,"professionalId":3,"appointmentDateTime":"2026-11-20T15:00:00Z","appointmentType":1,"status":"PENDING","reviewed":false}"""
        ))
        val repo = AppointmentRepositoryImpl(api, sessions, cache)
        val result = repo.book(BookingRequest(3, LocalDate.of(2026, 11, 20), "10:00", 1, "Primera vez"))

        val appointment = (result as Outcome.Success).data
        assertEquals(AppointmentStatus.PENDING, appointment.status)
        assertEquals(Instant.parse("2026-11-20T15:00:00Z"), appointment.dateTime)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"appointmentDateTime\":\"2026-11-20T15:00:00Z\""))
        assertTrue(body.contains("\"patientId\":5"))
    }

    @Test
    fun book_409_slotTaken() = runTest {
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"message":"El horario ya está ocupado"}"""))
        val repo = AppointmentRepositoryImpl(api, sessions, cache)
        val result = repo.book(BookingRequest(3, LocalDate.of(2026, 11, 20), "10:00", 1, null))
        val error = (result as Outcome.Failure).error
        assertTrue(error is AppError.Conflict)
        assertEquals("El horario ya está ocupado", error.message)
    }

    @Test
    fun getMine_usesPatientQuery_andCachesResult() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """[{"id":1,"patientId":5,"professionalId":3,"appointmentDateTime":"2026-11-20T15:00:00Z","appointmentType":1,"status":"CONFIRMED","reviewed":false}]"""
        ))
        val repo = AppointmentRepositoryImpl(api, sessions, cache)
        val result = repo.getMine()
        assertEquals(1, (result as Outcome.Success).data.size)
        assertEquals("/api/v1/appointments?patientId=5", server.takeRequest().path)
        coVerify { cache.replaceAll(match { it.size == 1 && it[0].status == "CONFIRMED" }) }
    }

    @Test
    fun updateStatus_sendsUppercaseStatus() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"id":1,"patientId":5,"professionalId":3,"appointmentDateTime":"2026-11-20T15:00:00Z","status":"CANCELLED"}"""
        ))
        val repo = AppointmentRepositoryImpl(api, sessions, cache)
        repo.updateStatus(1, AppointmentStatus.CANCELLED)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/appointments/1/status", request.path)
        assertEquals("""{"status":"CANCELLED"}""", request.body.readUtf8())
    }

    @Test
    fun trigger_inTheFuture_isRejectedWithoutCallingServer() = runTest {
        val repo = StressRepositoryImpl(api, sessions)
        val result = repo.addTrigger("work", 5, "x", Instant.now().plusSeconds(3600))
        assertTrue((result as Outcome.Failure).error is AppError.Validation)
        assertEquals(0, server.requestCount)
    }
}
