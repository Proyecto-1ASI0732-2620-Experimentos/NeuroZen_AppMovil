package com.example.neurozen_front.domain

import com.example.neurozen_front.data.remote.MyProfessionalDto
import com.example.neurozen_front.data.remote.ProfessionalDto
import com.example.neurozen_front.data.remote.ScheduleDto
import com.example.neurozen_front.data.remote.toDomain
import com.example.neurozen_front.domain.logic.AvailabilityDraft
import com.example.neurozen_front.domain.logic.Catalogs
import com.example.neurozen_front.domain.logic.DayCodec
import com.example.neurozen_front.domain.logic.Validators
import com.example.neurozen_front.domain.logic.YouTube
import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.VerificationStatus
import com.example.neurozen_front.domain.model.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/** Lógica nueva usada por el registro, el perfil del psicólogo y la biblioteca. */
class NewLogicCoverageTest {

    private fun myProfile(schedule: ScheduleDto?) = MyProfessionalDto(
        7, 21, "Damián Inga", "damian@correo.com", "clinical", "98563", "VERIFIED", null, null,
        null, null, null, null, schedule, null, null
    )

    @Test
    fun myProfessional_withoutDays_hasNoSchedule() {
        assertNull(myProfile(null).toDomain().schedule)
        assertNull(myProfile(ScheduleDto(emptyList(), "09:00", "18:00")).toDomain().schedule)
        val profile = myProfile(ScheduleDto(listOf("monday"), "09:00", "13:00")).toDomain()
        assertEquals(listOf("monday"), profile.schedule?.days)
        assertEquals(VerificationStatus.VERIFIED, profile.status)
        assertEquals("", profile.bio)
        assertEquals(0.0, profile.rating, 0.0)
    }

    @Test
    fun professional_onlyKeepsHttpsImages() {
        fun dto(image: String?) = ProfessionalDto(1, 2, "Ana", "clinical", null, 3, 50.0, null, null, null, image, "VERIFIED")
        assertNull(dto("http://inseguro.com/a.png").toDomain().imageUrl)
        assertEquals("https://cdn.com/a.png", dto("https://cdn.com/a.png").toDomain().imageUrl)
        assertEquals(0, dto(null).toDomain().reviews)
    }

    @Test
    fun availability_summaryAndDayStyles() {
        val weekdays = AvailabilityDraft()
        assertEquals("Lunes a Viernes, 09:00 - 18:00", weekdays.summary())
        val split = AvailabilityDraft(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), "08:00", "12:00")
        assertEquals("Lunes, Miércoles, 08:00 - 12:00", split.summary())
        assertEquals("", DayCodec.summary(emptySet(), "09:00", "18:00"))
        assertEquals("MON", DayCodec.encode(DayOfWeek.MONDAY, DayCodec.Style.ENGLISH_SHORT))
        assertEquals("Lunes", DayCodec.encode(DayOfWeek.MONDAY, DayCodec.Style.SPANISH))
    }

    @Test
    fun catalogs_labelsWithFallbacks() {
        assertEquals("Avanzado", Catalogs.planName(2))
        assertEquals("Básico", Catalogs.planName(null))
        assertEquals("Formal", Catalogs.workTypeLabel("FORMAL"))
        assertEquals("otro", Catalogs.workTypeLabel("otro"))
        assertNull(Catalogs.workTypeLabel(null))
        assertEquals("Trabajo", Catalogs.categoryLabel("work"))
        assertEquals("Familia", Catalogs.categoryLabel("familia"))
    }

    @Test
    fun youtube_watchUrlAndHours() {
        val id = YouTube.videoId("https://youtu.be/CVMBSsqTGOk")!!
        assertEquals("https://www.youtube.com/watch?v=CVMBSsqTGOk", YouTube.watchUrl(id))
        assertTrue(Validators.isValidHour("09:30"))
        assertFalse(Validators.isValidHour("25:00"))
    }

    @Test
    fun outcome_mapKeepsFailures() {
        assertEquals(Outcome.Success(4), Outcome.Success(2).map { it * 2 })
        val failure: Outcome<Int> = Outcome.Failure(AppError.Server("caído"))
        assertSame(failure, failure.map { it * 2 })
    }
}
