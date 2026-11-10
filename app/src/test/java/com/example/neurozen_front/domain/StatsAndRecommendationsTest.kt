package com.example.neurozen_front.domain

import com.example.neurozen_front.domain.logic.RecommendationAction
import com.example.neurozen_front.domain.logic.RecommendationEngine
import com.example.neurozen_front.domain.logic.StressStatsCalculator
import com.example.neurozen_front.domain.model.Assessment
import com.example.neurozen_front.domain.model.StressLevel
import com.example.neurozen_front.domain.model.StressTrigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class StatsAndRecommendationsTest {
    private val now = Instant.parse("2026-11-15T20:00:00Z")

    private fun trigger(id: Long, level: Int, at: String, category: String = "work") =
        StressTrigger(id, "", category, level, "", Instant.parse(at))

    @Test
    fun emptyData_givesZeros() {
        val stats = StressStatsCalculator.calculate(emptyList(), emptyList(), now)
        assertEquals(0.0, stats.current, 0.0)
        assertEquals(0, stats.triggersCount)
        assertEquals("Sin datos", stats.peakHours)
        assertNull(stats.lastAssessment)
    }

    @Test
    fun stats_currentAverageWeeklyChangePeakAndCategory() {
        val triggers = listOf(
            trigger(1, 8, "2026-11-15T15:10:00Z"),               // this week, 10:00 Lima
            trigger(2, 6, "2026-11-14T15:40:00Z"),               // this week, 10:00 Lima
            trigger(3, 4, "2026-11-05T20:00:00Z", "personal"),   // last week
            trigger(4, 2, "2026-11-04T20:00:00Z", "personal")    // last week
        )
        val assessments = listOf(Assessment(1, emptyList(), 22, StressLevel.MODERATE, Instant.parse("2026-11-12T12:00:00Z")))
        val stats = StressStatsCalculator.calculate(triggers, assessments, now)
        assertEquals(8.0, stats.current, 0.0)
        assertEquals(5.0, stats.average, 0.0)
        assertEquals(4.0, stats.weeklyChange, 0.0) // 7.0 - 3.0
        assertEquals("10:00 - 11:00", stats.peakHours)
        assertEquals(4, stats.triggersCount)
        assertEquals(22, stats.lastAssessment?.score)
    }

    @Test
    fun highLevel_recommendsProfessional() {
        val recs = RecommendationEngine.recommend(StressLevel.HIGH, null, null, hasAssessment = true)
        assertEquals(RecommendationAction.PROFESSIONALS, recs.first().action)
    }

    @Test
    fun noAssessment_recommendsTestFirst() {
        val recs = RecommendationEngine.recommend(null, null, null, hasAssessment = false)
        assertEquals(RecommendationAction.ASSESSMENT, recs.first().action)
    }

    @Test
    fun overtime_recommendsWorkload() {
        val recs = RecommendationEngine.recommend(StressLevel.LOW, "work", 52.0, hasAssessment = true)
        assertTrue(recs.any { it.title.contains("48 h") })
    }
}
