package com.example.neurozen_front.domain

import com.example.neurozen_front.domain.logic.AssessmentScoring
import com.example.neurozen_front.domain.model.StressLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssessmentScoringTest {

    @Test
    fun allZeros_reverseItemsCountAsFour() {
        // Questions 4, 5, 7 and 8 are reversed: 4 * 4 = 16
        assertEquals(16, AssessmentScoring.score(List(10) { 0 }))
    }

    @Test
    fun allFours_reverseItemsCountAsZero() {
        assertEquals(24, AssessmentScoring.score(List(10) { 4 }))
    }

    @Test
    fun maximumStress_is40() {
        val answers = List(10) { i -> if (i in AssessmentScoring.REVERSED_INDEXES) 0 else 4 }
        assertEquals(40, AssessmentScoring.score(answers))
        assertEquals(StressLevel.HIGH, AssessmentScoring.level(40))
    }

    @Test
    fun minimumStress_is0() {
        val answers = List(10) { i -> if (i in AssessmentScoring.REVERSED_INDEXES) 4 else 0 }
        assertEquals(0, AssessmentScoring.score(answers))
        assertEquals(StressLevel.LOW, AssessmentScoring.level(0))
    }

    @Test
    fun levelThresholds() {
        assertEquals(StressLevel.LOW, AssessmentScoring.level(13))
        assertEquals(StressLevel.MODERATE, AssessmentScoring.level(14))
        assertEquals(StressLevel.MODERATE, AssessmentScoring.level(26))
        assertEquals(StressLevel.HIGH, AssessmentScoring.level(27))
    }

    @Test
    fun incompleteOrOutOfRange_isRejected() {
        assertFalse(AssessmentScoring.isComplete(List(9) { 1 }))
        assertFalse(AssessmentScoring.isComplete(List(10) { if (it == 3) null else 1 }))
        assertFalse(AssessmentScoring.isComplete(List(10) { 5 }))
        assertTrue(AssessmentScoring.isComplete(List(10) { 2 }))
    }

    @Test(expected = IllegalArgumentException::class)
    fun score_throwsOnWrongSize() {
        AssessmentScoring.score(listOf(1, 2, 3))
    }
}
