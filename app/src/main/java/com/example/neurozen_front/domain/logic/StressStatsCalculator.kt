package com.example.neurozen_front.domain.logic

import com.example.neurozen_front.domain.model.Assessment
import com.example.neurozen_front.domain.model.LastAssessmentSummary
import com.example.neurozen_front.domain.model.StressStats
import com.example.neurozen_front.domain.model.StressTrigger
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.round

/** Builds the dashboard numbers and the progress report shared with the psychologist. */
object StressStatsCalculator {

    fun calculate(
        triggers: List<StressTrigger>,
        assessments: List<Assessment>,
        now: Instant,
        zone: ZoneId = DateTimeUtils.LIMA
    ): StressStats {
        val sorted = triggers.sortedByDescending { it.triggeredAt }
        val current = sorted.firstOrNull()?.stressLevel?.toDouble() ?: 0.0
        val average = if (sorted.isEmpty()) 0.0 else round1(sorted.map { it.stressLevel }.average())

        val weekAgo = now.minus(Duration.ofDays(7))
        val twoWeeksAgo = now.minus(Duration.ofDays(14))
        val thisWeek = sorted.filter { it.triggeredAt.isAfter(weekAgo) }.map { it.stressLevel }
        val lastWeek = sorted.filter { it.triggeredAt.isAfter(twoWeeksAgo) && !it.triggeredAt.isAfter(weekAgo) }.map { it.stressLevel }
        val weeklyChange = if (thisWeek.isEmpty() || lastWeek.isEmpty()) 0.0
        else round1(thisWeek.average() - lastWeek.average())

        val peakHours = sorted
            .groupingBy { it.triggeredAt.atZone(zone).hour }
            .eachCount()
            .maxWithOrNull(compareBy<Map.Entry<Int, Int>> { it.value }.thenBy { -it.key })
            ?.key?.let { "%02d:00 - %02d:00".format(it, (it + 1) % 24) } ?: "Sin datos"

        val topCategory = sorted
            .groupingBy { it.category }
            .eachCount()
            .maxByOrNull { it.value }?.key ?: "Sin datos"

        val last = assessments.maxByOrNull { it.completedAt }
            ?.let { LastAssessmentSummary(it.score, it.level, it.completedAt) }

        return StressStats(
            current = current,
            average = average,
            weeklyChange = weeklyChange,
            peakHours = peakHours,
            triggersCount = sorted.size,
            topCategory = topCategory,
            lastAssessment = last
        )
    }

    private fun round1(value: Double): Double = round(value * 10) / 10.0
}
