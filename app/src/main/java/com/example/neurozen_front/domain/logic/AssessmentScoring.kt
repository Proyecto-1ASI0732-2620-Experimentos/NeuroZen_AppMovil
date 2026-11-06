package com.example.neurozen_front.domain.logic

import com.example.neurozen_front.domain.model.StressLevel

/**
 * Mirrors the backend scoring of the 10-question perceived stress test (answers 0-4).
 * Questions 4, 5, 7 and 8 are reverse-scored. The server result is the source of truth;
 * this is used to validate before sending and to preview offline.
 */
object AssessmentScoring {
    const val QUESTION_COUNT = 10
    const val MIN_ANSWER = 0
    const val MAX_ANSWER = 4

    /** Zero-based indexes of questions 4, 5, 7 and 8. */
    val REVERSED_INDEXES = setOf(3, 4, 6, 7)

    fun isComplete(answers: List<Int?>): Boolean =
        answers.size == QUESTION_COUNT && answers.all { it != null && it in MIN_ANSWER..MAX_ANSWER }

    fun score(answers: List<Int>): Int {
        require(answers.size == QUESTION_COUNT) { "Se requieren $QUESTION_COUNT respuestas" }
        require(answers.all { it in MIN_ANSWER..MAX_ANSWER }) { "Respuestas fuera de rango" }
        return answers.mapIndexed { index, answer ->
            if (index in REVERSED_INDEXES) MAX_ANSWER - answer else answer
        }.sum()
    }

    fun level(score: Int): StressLevel = when {
        score <= 13 -> StressLevel.LOW
        score <= 26 -> StressLevel.MODERATE
        else -> StressLevel.HIGH
    }
}
