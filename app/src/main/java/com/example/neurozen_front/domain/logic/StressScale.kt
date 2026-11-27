package com.example.neurozen_front.domain.logic

import com.example.neurozen_front.domain.model.StressLevel
import kotlin.math.roundToInt

/**
 * Single 0-100 stress index used only for PRESENTATION (same normalisation as the web).
 * It never changes the stored data, what is sent to the backend or the PSS-10 scoring.
 *
 * - PSS-10 score 0-40  -> round(score / 40 x 100)
 * - Trigger level 1-10 -> level x 10 (10-100)
 */
object StressScale {
    const val MAX = 100
    const val PSS_MAX = 40
    const val TRIGGER_MIN = 1
    const val TRIGGER_MAX = 10

    /** Index thresholds that match both sources: PSS low <= 13 (33), trigger low <= 3 (30); PSS moderate <= 26 (65), trigger <= 6 (60). */
    private const val LOW_MAX = 33
    private const val MODERATE_MAX = 66

    /** 0-100 index of a PSS-10 score, or null when the score is outside 0-40. */
    fun fromPss(score: Int): Int? =
        if (score in 0..PSS_MAX) (score * MAX.toDouble() / PSS_MAX).roundToInt() else null

    /** 0-100 index of a trigger stress level, or null when the level is outside 1-10. */
    fun fromTrigger(level: Int): Int? =
        if (level in TRIGGER_MIN..TRIGGER_MAX) level * (MAX / TRIGGER_MAX) else null

    /** 0-100 index of an average trigger level (e.g. 6.4 -> 64), or null when outside 1-10. */
    fun fromTriggerAverage(average: Double): Int? =
        if (average >= TRIGGER_MIN && average <= TRIGGER_MAX) (average * (MAX / TRIGGER_MAX)).roundToInt() else null

    /** Level of an index; consistent with the PSS-10 (0-13/14-26/27-40) and trigger (1-3/4-6/7-10) thresholds. */
    fun levelOf(index: Int): StressLevel? = when (index) {
        in 0..LOW_MAX -> StressLevel.LOW
        in (LOW_MAX + 1)..MODERATE_MAX -> StressLevel.MODERATE
        in (MODERATE_MAX + 1)..MAX -> StressLevel.HIGH
        else -> null
    }
}
