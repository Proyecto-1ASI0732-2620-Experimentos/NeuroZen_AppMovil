package com.example.neurozen_front.domain

import com.example.neurozen_front.R
import com.example.neurozen_front.domain.logic.BookingCalendar
import com.example.neurozen_front.domain.logic.CalendarSource
import com.example.neurozen_front.domain.logic.ProfessionalFormat
import com.example.neurozen_front.domain.logic.RatingDisplay
import com.example.neurozen_front.domain.logic.Validators
import com.example.neurozen_front.domain.model.AppointmentStatus
import com.example.neurozen_front.domain.model.AvailabilityCalendar
import com.example.neurozen_front.domain.model.AvailabilitySchedule
import com.example.neurozen_front.domain.model.StressLevel
import com.example.neurozen_front.presentation.components.AppointmentStatusStyles
import com.example.neurozen_front.presentation.components.Contrast
import com.example.neurozen_front.presentation.components.StatusIcon
import com.example.neurozen_front.presentation.components.UiMappings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/** M1: status chips with icon, text, accessible label and contrast >= 4.5:1. */
class AppointmentStatusStyleTest {

    @Test
    fun everyStatus_hasItsOwnLabelAndIcon() {
        val expected = mapOf(
            AppointmentStatus.PENDING to (R.string.status_pending to StatusIcon.CLOCK),
            AppointmentStatus.CONFIRMED to (R.string.status_confirmed to StatusIcon.CHECK),
            AppointmentStatus.CANCELLED to (R.string.status_cancelled to StatusIcon.CLOSE),
            AppointmentStatus.COMPLETED to (R.string.status_completed to StatusIcon.DONE_ALL),
            AppointmentStatus.UNKNOWN to (R.string.status_unknown to StatusIcon.HELP)
        )
        expected.forEach { (status, pair) ->
            val style = AppointmentStatusStyles.of(status)
            assertEquals(pair.first, style.label)
            assertEquals(pair.second, style.icon)
        }
        val realStates = listOf(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED)
        assertEquals(4, realStates.map { AppointmentStatusStyles.of(it).icon }.toSet().size)
    }

    @Test
    fun textContrast_isAtLeast4_5_inLightAndDarkThemes() {
        AppointmentStatus.entries.forEach { status ->
            val s = AppointmentStatusStyles.of(status)
            assertTrue("$status light", Contrast.ratio(s.lightContent, s.lightContainer) >= 4.5)
            assertTrue("$status dark", Contrast.ratio(s.darkContent, s.darkContainer) >= 4.5)
        }
    }

    @Test
    fun contrast_knownValues() {
        assertEquals(21.0, Contrast.ratio(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), 0.01)
        assertEquals(1.0, Contrast.ratio(0xFF2D5A4A.toInt(), 0xFF2D5A4A.toInt()), 0.0001)
        assertTrue(Contrast.ratio(0xFF2D5A4A.toInt(), 0xFFFFFFFF.toInt()) >= 4.5) // NeuroZen green on white
    }

    @Test
    fun stressLevelLabels_andPlanPrices_comeFromStringResources() {
        assertEquals(R.string.stress_level_low, UiMappings.stressLevelLabel(StressLevel.LOW))
        assertEquals(R.string.stress_level_moderate, UiMappings.stressLevelLabel(StressLevel.MODERATE))
        assertEquals(R.string.stress_level_high, UiMappings.stressLevelLabel(StressLevel.HIGH))
        assertEquals(R.string.plan_price_basic, UiMappings.planPrice(1))
        assertEquals(R.string.plan_price_advanced, UiMappings.planPrice(2))
        assertEquals(R.string.plan_price_professional, UiMappings.planPrice(3))
        assertEquals(R.string.plan_price_basic, UiMappings.planPrice(99))
    }
}

/** B1: no filler values for price and rating. */
class ProfessionalFormatTest {

    @Test
    fun rating_zeroOrNoReviews_isNone() {
        assertEquals(RatingDisplay.None, ProfessionalFormat.rating(0.0, 0))
        assertEquals(RatingDisplay.None, ProfessionalFormat.rating(4.5, 0))
        assertEquals(RatingDisplay.None, ProfessionalFormat.rating(0.0, 3))
        assertEquals(RatingDisplay.None, ProfessionalFormat.rating(Double.NaN, 3))
    }

    @Test
    fun rating_usesDecimalComma_andCapsAtFive() {
        assertEquals(RatingDisplay.Rated(4.5, "4,5", 12), ProfessionalFormat.rating(4.5, 12))
        assertEquals("4,7", (ProfessionalFormat.rating(4.66, 3) as RatingDisplay.Rated).text)
        assertEquals("5,0", (ProfessionalFormat.rating(7.0, 1) as RatingDisplay.Rated).text)
    }

    @Test
    fun price_nullOrZero_isUndefined_neverZero() {
        assertNull(ProfessionalFormat.priceAmount(null))
        assertNull(ProfessionalFormat.priceAmount(0.0))
        assertNull(ProfessionalFormat.priceAmount(-10.0))
        assertNull(ProfessionalFormat.priceAmount(Double.NaN))
        assertNull(ProfessionalFormat.priceAmount(Double.POSITIVE_INFINITY))
        assertEquals("120.00", ProfessionalFormat.priceAmount(120.0))
        assertEquals("85.50", ProfessionalFormat.priceAmount(85.5))
    }
}

/** B2: only days with free hours are enabled. */
class BookingCalendarTest {
    private val today = LocalDate.of(2026, 11, 30) // Monday

    @Test
    fun fromAvailability_enablesOnlyDaysWithSlots_andUsesNextAvailableDate() {
        val calendar = AvailabilityCalendar(
            "America/Lima", 60,
            mapOf(today to emptyList(), today.plusDays(2) to listOf("09:00", "10:00"), today.plusDays(5) to listOf("15:00")),
            today.plusDays(2)
        )
        val state = BookingCalendar.fromAvailability(calendar, today)
        assertEquals(BookingCalendar.WINDOW_DAYS, state.days.size)
        assertEquals(today, state.days.first().date)
        assertEquals(listOf(today.plusDays(2), today.plusDays(5)), state.days.filter { it.enabled }.map { it.date })
        assertEquals(today.plusDays(2), state.nextAvailable)
        assertEquals(CalendarSource.BACKEND, state.source)
        assertTrue(state.hasEnabledDays)
        assertEquals(today.plusDays(29), BookingCalendar.endDate(today))
    }

    @Test
    fun fromAvailability_withoutNextDate_usesFirstEnabled_andIgnoresPastNext() {
        val cal = AvailabilityCalendar("America/Lima", null, mapOf(today.plusDays(3) to listOf("11:00")), today.minusDays(1))
        assertEquals(today.plusDays(3), BookingCalendar.fromAvailability(cal, today).nextAvailable)
        val empty = BookingCalendar.fromAvailability(AvailabilityCalendar("America/Lima", null, emptyMap(), null), today, windowDays = 7)
        assertFalse(empty.hasEnabledDays)
        assertNull(empty.nextAvailable)
    }

    @Test
    fun fromAvailability_nextDateBeyondWindow_isStillOffered() {
        val cal = AvailabilityCalendar("America/Lima", 60, emptyMap(), today.plusDays(45))
        val state = BookingCalendar.fromAvailability(cal, today)
        assertFalse(state.hasEnabledDays)
        assertEquals(today.plusDays(45), state.nextAvailable)
    }

    @Test
    fun fromSchedule_enablesTheProfessionalWeekdays() {
        val state = BookingCalendar.fromSchedule(AvailabilitySchedule(listOf("monday", "wednesday", "Lunes"), "09:00", "13:00"), today, windowDays = 7)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), state.days.filter { it.enabled }.map { it.date.dayOfWeek }.toSet())
        assertEquals(today, state.nextAvailable)
        assertEquals(CalendarSource.SCHEDULE, state.source)
    }

    @Test
    fun monthShort_isSpanishWithoutDot() {
        val month = com.example.neurozen_front.domain.logic.DateTimeUtils.monthShort(LocalDate.of(2026, 12, 1))
        assertTrue(month.startsWith("dic"))
        assertFalse(month.endsWith("."))
    }

    @Test
    fun unverified_enablesEverything_withoutNextDate() {
        val state = BookingCalendar.unverified(today, windowDays = 0)
        assertEquals(1, state.days.size)
        assertTrue(state.days.all { it.enabled })
        assertNull(state.nextAvailable)
        assertEquals(CalendarSource.UNVERIFIED, state.source)
    }
}

/** B3: checklist with exactly the backend PasswordPolicy. */
class PasswordChecklistTest {

    @Test
    fun checklist_matchesBackendPolicy() {
        assertEquals(Validators.PasswordChecklist(false, false, false, true), Validators.passwordChecklist(""))
        assertTrue(Validators.passwordChecklist("Clave2026").allMet)
        val onlyLetters = Validators.passwordChecklist("solamenteletras")
        assertTrue(onlyLetters.minLength && onlyLetters.hasLetter)
        assertFalse(onlyLetters.hasDigit || onlyLetters.allMet)
    }

    @Test
    fun unicodeLettersAndDigits_countLikeCharIsLetterAndIsDigit() {
        assertTrue(Validators.passwordChecklist("ñandú١٢٣٤٥").allMet) // Arabic-Indic digits are Unicode digits
        assertTrue(Validators.passwordChecklist("密码密码密码12").hasLetter)
    }

    @Test
    fun maxBytes_isUtf8_not_characters() {
        assertTrue(Validators.passwordChecklist("a1" + "b".repeat(70)).withinMaxBytes) // 72 bytes
        assertFalse(Validators.passwordChecklist("a1" + "b".repeat(71)).withinMaxBytes) // 73 bytes
        assertFalse(Validators.passwordChecklist("Ñ".repeat(36) + "1").withinMaxBytes) // 37 chars, 73 bytes
        // Same verdict as passwordError (used on submit)
        listOf("", "a1", "solamenteletras", "Clave2026", "Ñ".repeat(40) + "1").forEach {
            assertEquals(it, Validators.passwordError(it) == null, Validators.passwordChecklist(it).allMet)
        }
    }
}
