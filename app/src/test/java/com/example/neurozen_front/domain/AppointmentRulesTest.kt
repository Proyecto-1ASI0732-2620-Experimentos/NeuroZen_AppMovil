package com.example.neurozen_front.domain

import com.example.neurozen_front.domain.logic.AppointmentRules
import com.example.neurozen_front.domain.model.Appointment
import com.example.neurozen_front.domain.model.AppointmentStatus
import com.example.neurozen_front.domain.model.AppointmentStatus.CANCELLED
import com.example.neurozen_front.domain.model.AppointmentStatus.COMPLETED
import com.example.neurozen_front.domain.model.AppointmentStatus.CONFIRMED
import com.example.neurozen_front.domain.model.AppointmentStatus.PENDING
import com.example.neurozen_front.domain.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AppointmentRulesTest {

    private fun appointment(status: AppointmentStatus, reviewed: Boolean = false, at: Instant = Instant.parse("2030-01-01T15:00:00Z")) =
        Appointment(1, 10, null, null, 20, at, 1, status, null, reviewed)

    @Test
    fun psychologist_canConfirmOrCancelPending() {
        assertEquals(setOf(CONFIRMED, CANCELLED), AppointmentRules.allowedTransitions(PENDING, Role.PSYCHOLOGIST))
    }

    @Test
    fun psychologist_canCompleteOrCancelConfirmed() {
        assertEquals(setOf(COMPLETED, CANCELLED), AppointmentRules.allowedTransitions(CONFIRMED, Role.PSYCHOLOGIST))
    }

    @Test
    fun user_canOnlyCancel() {
        assertTrue(AppointmentRules.canTransition(PENDING, CANCELLED, Role.USER))
        assertTrue(AppointmentRules.canTransition(CONFIRMED, CANCELLED, Role.USER))
        assertFalse(AppointmentRules.canTransition(PENDING, CONFIRMED, Role.USER))
        assertFalse(AppointmentRules.canTransition(CONFIRMED, COMPLETED, Role.USER))
    }

    @Test
    fun finalStates_haveNoTransitions() {
        Role.entries.forEach { role ->
            assertTrue(AppointmentRules.allowedTransitions(COMPLETED, role).isEmpty())
            assertTrue(AppointmentRules.allowedTransitions(CANCELLED, role).isEmpty())
        }
    }

    @Test
    fun pendingCannotJumpToCompleted() {
        assertFalse(AppointmentRules.canTransition(PENDING, COMPLETED, Role.PSYCHOLOGIST))
    }

    @Test
    fun admin_hasNoTransitionsOnMobile() {
        assertTrue(AppointmentRules.allowedTransitions(PENDING, Role.ADMIN).isEmpty())
    }

    @Test
    fun shareReport_onlyPendingOrConfirmed() {
        assertTrue(AppointmentRules.canShareReport(appointment(PENDING)))
        assertTrue(AppointmentRules.canShareReport(appointment(CONFIRMED)))
        assertFalse(AppointmentRules.canShareReport(appointment(COMPLETED)))
        assertFalse(AppointmentRules.canShareReport(appointment(CANCELLED)))
    }

    @Test
    fun review_onlyCompletedAndNotReviewed() {
        assertTrue(AppointmentRules.canReview(appointment(COMPLETED)))
        assertFalse(AppointmentRules.canReview(appointment(COMPLETED, reviewed = true)))
        assertFalse(AppointmentRules.canReview(appointment(CONFIRMED)))
    }

    @Test
    fun upcoming_requiresFutureAndActiveStatus() {
        val now = Instant.parse("2029-12-31T00:00:00Z")
        assertTrue(AppointmentRules.isUpcoming(appointment(CONFIRMED), now))
        assertFalse(AppointmentRules.isUpcoming(appointment(CANCELLED), now))
        assertFalse(AppointmentRules.isUpcoming(appointment(PENDING, at = Instant.parse("2020-01-01T00:00:00Z")), now))
    }

    @Test
    fun statusParsing_isCaseInsensitive() {
        assertEquals(CONFIRMED, AppointmentStatus.from("confirmed"))
        assertEquals(AppointmentStatus.UNKNOWN, AppointmentStatus.from("whatever"))
        assertEquals(Role.PSYCHOLOGIST, Role.from("Psychologist"))
        assertEquals(Role.UNKNOWN, Role.from(null))
    }
}
