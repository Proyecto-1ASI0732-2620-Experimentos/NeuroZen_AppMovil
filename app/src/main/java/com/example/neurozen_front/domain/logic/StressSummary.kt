package com.example.neurozen_front.domain.logic

import com.example.neurozen_front.domain.model.Assessment
import com.example.neurozen_front.domain.model.StressLevel
import com.example.neurozen_front.domain.model.StressTrigger
import java.time.Instant

/** Where a point of the stress evolution comes from. */
enum class StressSource { ASSESSMENT, TRIGGER }

/** One real measurement on the 0-100 index, with its original value (26 of 40, 7 of 10). */
data class StressPoint(val at: Instant, val index: Int, val source: StressSource, val rawValue: Int)

/** Data of the Home summary card. Built only from the user's real PSS-10 results and triggers. */
data class StressSummary(
    val current: StressPoint,
    val level: StressLevel,
    val lastAssessment: StressPoint?,
    val lastTrigger: StressPoint?,
    /** Chronological (oldest first), at most [StressSummaryBuilder.MAX_POINTS]. */
    val evolution: List<StressPoint>
)

object StressSummaryBuilder {
    const val MAX_POINTS = 10

    private val chronological = compareBy<StressPoint>({ it.at }, { it.source.ordinal })

    /** Returns null when there is no valid PSS-10 nor trigger: the UI then shows the welcome state, never sample data. */
    fun build(triggers: List<StressTrigger>, assessments: List<Assessment>, maxPoints: Int = MAX_POINTS): StressSummary? {
        val assessmentPoints = assessments.mapNotNull { a ->
            StressScale.fromPss(a.score)?.let { StressPoint(a.completedAt, it, StressSource.ASSESSMENT, a.score) }
        }
        val triggerPoints = triggers.mapNotNull { t ->
            StressScale.fromTrigger(t.stressLevel)?.let { StressPoint(t.triggeredAt, it, StressSource.TRIGGER, t.stressLevel) }
        }
        val all = (assessmentPoints + triggerPoints).sortedWith(chronological)
        val current = all.lastOrNull() ?: return null
        val level = StressScale.levelOf(current.index) ?: return null
        return StressSummary(
            current = current,
            level = level,
            lastAssessment = assessmentPoints.maxWithOrNull(chronological),
            lastTrigger = triggerPoints.maxWithOrNull(chronological),
            evolution = all.takeLast(maxPoints.coerceAtLeast(1))
        )
    }
}
