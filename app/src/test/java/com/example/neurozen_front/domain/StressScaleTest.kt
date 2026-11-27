package com.example.neurozen_front.domain

import com.example.neurozen_front.domain.logic.StressScale
import com.example.neurozen_front.domain.logic.StressSource
import com.example.neurozen_front.domain.logic.StressSummaryBuilder
import com.example.neurozen_front.domain.model.Assessment
import com.example.neurozen_front.domain.model.StressLevel
import com.example.neurozen_front.domain.model.StressTrigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** M2: single 0-100 index (presentation only). */
class StressScaleTest {

    @Test
    fun pss_limitsAndRounding() {
        assertEquals(0, StressScale.fromPss(0))
        assertEquals(100, StressScale.fromPss(40))
        assertEquals(65, StressScale.fromPss(26))
        assertEquals(33, StressScale.fromPss(13)) // 32.5 rounds half up, like Math.round in the web
        assertEquals(35, StressScale.fromPss(14))
        assertEquals(3, StressScale.fromPss(1))
    }

    @Test
    fun pss_invalidValues_returnNull() {
        assertNull(StressScale.fromPss(-1))
        assertNull(StressScale.fromPss(41))
        assertNull(StressScale.fromPss(Int.MIN_VALUE))
    }

    @Test
    fun trigger_limits() {
        assertEquals(10, StressScale.fromTrigger(1))
        assertEquals(100, StressScale.fromTrigger(10))
        assertEquals(70, StressScale.fromTrigger(7))
        assertNull(StressScale.fromTrigger(0))
        assertNull(StressScale.fromTrigger(11))
        assertNull(StressScale.fromTrigger(-5))
    }

    @Test
    fun triggerAverage_roundsAndRejectsOutOfRange() {
        assertEquals(64, StressScale.fromTriggerAverage(6.4))
        assertEquals(10, StressScale.fromTriggerAverage(1.0))
        assertEquals(100, StressScale.fromTriggerAverage(10.0))
        assertNull(StressScale.fromTriggerAverage(0.0))
        assertNull(StressScale.fromTriggerAverage(10.5))
        assertNull(StressScale.fromTriggerAverage(Double.NaN))
    }

    @Test
    fun levelOf_matchesPssAndTriggerThresholds() {
        // PSS-10: low 0-13, moderate 14-26, high 27-40
        assertEquals(StressLevel.LOW, StressScale.levelOf(StressScale.fromPss(13)!!))
        assertEquals(StressLevel.MODERATE, StressScale.levelOf(StressScale.fromPss(14)!!))
        assertEquals(StressLevel.MODERATE, StressScale.levelOf(StressScale.fromPss(26)!!))
        assertEquals(StressLevel.HIGH, StressScale.levelOf(StressScale.fromPss(27)!!))
        // Triggers: low 1-3, moderate 4-6, high 7-10
        for (level in 1..10) {
            assertEquals(StressLevel.fromTriggerLevel(level), StressScale.levelOf(StressScale.fromTrigger(level)!!))
        }
        assertNull(StressScale.levelOf(-1))
        assertNull(StressScale.levelOf(101))
    }
}

class StressSummaryBuilderTest {
    private val t0 = Instant.parse("2026-11-01T15:00:00Z")

    private fun trigger(id: Long, level: Int, minutes: Long) = StressTrigger(id, "", "work", level, "", t0.plusSeconds(minutes * 60))
    private fun assessment(id: Long, score: Int, minutes: Long) =
        Assessment(id, List(10) { 0 }, score, StressLevel.MODERATE, t0.plusSeconds(minutes * 60))

    @Test
    fun noData_returnsNull_soHomeShowsWelcomeInsteadOfSampleData() {
        assertNull(StressSummaryBuilder.build(emptyList(), emptyList()))
    }

    @Test
    fun invalidValuesOnly_returnsNull() {
        assertNull(StressSummaryBuilder.build(listOf(trigger(1, 0, 0)), listOf(assessment(1, 99, 0))))
    }

    @Test
    fun current_isTheMostRecentMeasurement_withBothDetails() {
        val summary = StressSummaryBuilder.build(
            triggers = listOf(trigger(1, 7, 10), trigger(2, 4, 30)),
            assessments = listOf(assessment(1, 26, 20))
        )!!
        assertEquals(StressSource.TRIGGER, summary.current.source)
        assertEquals(40, summary.current.index)
        assertEquals(StressLevel.MODERATE, summary.level)
        assertEquals(26, summary.lastAssessment!!.rawValue)
        assertEquals(65, summary.lastAssessment!!.index)
        assertEquals(4, summary.lastTrigger!!.rawValue)
        assertEquals(listOf(70, 65, 40), summary.evolution.map { it.index })
    }

    @Test
    fun evolution_keepsTheLastPointsInChronologicalOrder() {
        val triggers = (1..15).map { trigger(it.toLong(), (it % 10) + 1, it.toLong()) }
        val summary = StressSummaryBuilder.build(triggers.shuffled(java.util.Random(1)), emptyList(), maxPoints = 5)!!
        assertEquals(5, summary.evolution.size)
        assertEquals(summary.evolution.sortedBy { it.at }, summary.evolution)
        assertEquals(triggers.last().triggeredAt, summary.current.at)
        assertNull(summary.lastAssessment)
    }

    @Test
    fun sameInstant_assessmentComesFirst_triggerIsCurrent() {
        val summary = StressSummaryBuilder.build(listOf(trigger(1, 9, 0)), listOf(assessment(1, 4, 0)))!!
        assertEquals(StressSource.TRIGGER, summary.current.source)
        assertEquals(StressLevel.HIGH, summary.level)
    }
}
