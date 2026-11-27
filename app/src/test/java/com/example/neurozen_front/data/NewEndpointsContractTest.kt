package com.example.neurozen_front.data

import com.example.neurozen_front.data.local.AppointmentCacheDao
import com.example.neurozen_front.data.remote.ErrorMapper
import com.example.neurozen_front.data.remote.NeuroZenApi
import com.example.neurozen_front.data.repository.AppointmentRepositoryImpl
import com.example.neurozen_front.data.repository.LibraryRepositoryImpl
import com.example.neurozen_front.data.repository.ProfessionalRepositoryImpl
import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.repository.SessionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate

/** Contract of the new endpoints (section 3): availability, slots with duration, resource search, schedule. */
class NewEndpointsContractTest {
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
    fun availability_queryAndMapping() = runTest {
        server.enqueue(MockResponse().setBody(
            """{"professionalId":3,"timeZone":"America/Lima","slotMinutes":60,
               "days":[{"date":"2026-12-01","slots":["10:00","09:00"]},{"date":"2026-12-02","slots":[]},{"date":"bad","slots":["09:00"]}],
               "nextAvailableDate":"2026-12-01"}"""
        ))
        val repo = AppointmentRepositoryImpl(api, sessions, cache)
        val result = repo.getAvailability(3, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 30), 45)
        val cal = (result as Outcome.Success).data
        assertEquals("/api/v1/appointments/availability?professionalId=3&startDate=2026-12-01&endDate=2026-12-30&durationMinutes=45",
            server.takeRequest().path)
        assertEquals(listOf("09:00", "10:00"), cal.days[LocalDate.of(2026, 12, 1)])
        assertEquals(emptyList<String>(), cal.days[LocalDate.of(2026, 12, 2)])
        assertEquals(2, cal.days.size)
        assertEquals(LocalDate.of(2026, 12, 1), cal.nextAvailableDate)
        assertEquals("America/Lima", cal.timeZone)
        assertEquals(60, cal.slotMinutes)
    }

    @Test
    fun availability_missingTimeZone_defaultsToLima() = runTest {
        server.enqueue(MockResponse().setBody("""{"days":null,"nextAvailableDate":null}"""))
        val cal = (AppointmentRepositoryImpl(api, sessions, cache).getAvailability(3, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2), null)
            as Outcome.Success).data
        assertEquals("America/Lima", cal.timeZone)
        assertTrue(cal.days.isEmpty())
        assertNull(cal.nextAvailableDate)
        assertEquals("/api/v1/appointments/availability?professionalId=3&startDate=2026-12-01&endDate=2026-12-02", server.takeRequest().path)
    }

    @Test
    fun availableSlots_sendsDurationWhenKnown() = runTest {
        server.enqueue(MockResponse().setBody("""["11:00","09:00"]"""))
        val repo = ProfessionalRepositoryImpl(api)
        val result = repo.getAvailableSlots(3, LocalDate.of(2026, 12, 1), 30)
        assertEquals(listOf("09:00", "11:00"), (result as Outcome.Success).data)
        assertEquals("/api/v1/professionals/3/available-slots?date=2026-12-01&durationMinutes=30", server.takeRequest().path)
    }

    @Test
    fun professional_mapsScheduleAndNullPrice() = runTest {
        server.enqueue(MockResponse().setBody(
            """{"id":3,"userId":9,"name":"Ana Pérez","specialty":"clinical","price":null,"rating":0,"reviews":0,"image":"",
               "availabilitySchedule":{"days":["monday","friday"],"start":"09:00","end":"13:00"}}"""
        ))
        val p = (ProfessionalRepositoryImpl(api).getById(3) as Outcome.Success).data
        assertNull(p.price)
        assertNull(p.imageUrl)
        assertEquals(listOf("monday", "friday"), p.schedule?.days)
        assertEquals(0.0, p.rating, 0.0)
    }

    @Test
    fun resourceSearch_usesDurationMinutes_andLimit() = runTest {
        server.enqueue(MockResponse().setBody(
            """{"items":[{"id":7,"title":"Respira","resourceType":"Video","contentUrl":"https://www.youtube.com/watch?v=abcdefghijk",
               "durationMinutes":6,"thumbnail":"","tags":["respiracion"],"score":0.9}]}"""
        ))
        val result = LibraryRepositoryImpl(api).search("respiración", 30)
        val item = (result as Outcome.Success).data.single()
        assertEquals(6, item.durationMinutes)
        assertEquals("Video", item.type)
        assertTrue(item.thumbnail!!.startsWith("https://"))
        val path = server.takeRequest().path!!
        assertTrue(path.startsWith("/api/v1/resource-libraries/search?q=respiraci"))
        assertTrue(path.endsWith("&limit=10"))
    }

    @Test
    fun resourceSearch_emptyItems() = runTest {
        server.enqueue(MockResponse().setBody("""{"items":null}"""))
        assertTrue((LibraryRepositoryImpl(api).search("x", 3) as Outcome.Success).data.isEmpty())
    }
}

/** B4: never "null"/"undefined" on screen; 429 shows the backend message. */
class ErrorMessageHardeningTest {

    @Test
    fun literalNullOrUndefined_usesOwnText() {
        assertEquals("Revisa los datos ingresados.", ErrorMapper.fromHttp(400, """{"message":"null"}""").message)
        assertEquals("No encontramos lo que buscabas.", ErrorMapper.fromHttp(404, """{"message":" undefined "}""").message)
        assertEquals("Campo inválido", ErrorMapper.fromHttp(400, """{"message":"","errors":{"x":["null","Campo inválido"]}}""").message)
    }

    @Test
    fun tooManyRequests_showsBackendMessage_orDefault() {
        val withMessage = ErrorMapper.fromHttp(429, """{"message":"Demasiados intentos, espera 1 minuto"}""")
        assertTrue(withMessage is AppError.TooManyRequests)
        assertEquals("Demasiados intentos, espera 1 minuto", withMessage.message)
        assertEquals(AppError.TooManyRequests().message, ErrorMapper.fromHttp(429, "no json").message)
    }
}
