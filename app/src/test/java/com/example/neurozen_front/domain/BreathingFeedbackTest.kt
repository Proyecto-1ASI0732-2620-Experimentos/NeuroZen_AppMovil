package com.example.neurozen_front.domain

import com.example.neurozen_front.domain.logic.BreathPhaseKind
import com.example.neurozen_front.domain.logic.BreathingFeedbackController
import com.example.neurozen_front.domain.logic.BreathingFeedbackPolicy
import com.example.neurozen_front.domain.logic.Catalogs
import com.example.neurozen_front.domain.logic.FeedbackDecision
import com.example.neurozen_front.domain.logic.FeedbackOutput
import com.example.neurozen_front.domain.logic.FeedbackPreferences
import com.example.neurozen_front.domain.logic.RingerMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A2: which cue is played depending on the phase, the phone mode and the user switches. */
class BreathingFeedbackTest {
    private val both = FeedbackPreferences(vibration = true, sound = true)

    @Test
    fun eachPhase_hasADistinctPattern() {
        assertEquals(listOf(0L, 120L), BreathingFeedbackPolicy.patternFor(BreathPhaseKind.INHALE)) // one pulse
        assertEquals(4, BreathingFeedbackPolicy.patternFor(BreathPhaseKind.HOLD).size) // two short pulses
        assertEquals(listOf(0L, 450L), BreathingFeedbackPolicy.patternFor(BreathPhaseKind.EXHALE)) // one long pulse
        val patterns = BreathPhaseKind.entries.map(BreathingFeedbackPolicy::patternFor).toSet()
        assertEquals(3, patterns.size)
        assertTrue(BreathingFeedbackPolicy.HOLD_PATTERN[1] < BreathingFeedbackPolicy.EXHALE_PATTERN[1])
    }

    @Test
    fun silentMode_neitherSoundNorVibration() {
        BreathPhaseKind.entries.forEach { assertEquals(FeedbackDecision.NONE, BreathingFeedbackPolicy.decide(it, RingerMode.SILENT, both)) }
    }

    @Test
    fun vibrateMode_onlyVibration() {
        val d = BreathingFeedbackPolicy.decide(BreathPhaseKind.HOLD, RingerMode.VIBRATE, both)
        assertEquals(BreathingFeedbackPolicy.HOLD_PATTERN, d.vibrationPattern)
        assertFalse(d.playTone)
    }

    @Test
    fun normalMode_followsTheUserSwitches() {
        val all = BreathingFeedbackPolicy.decide(BreathPhaseKind.INHALE, RingerMode.NORMAL, both)
        assertEquals(BreathingFeedbackPolicy.INHALE_PATTERN, all.vibrationPattern)
        assertTrue(all.playTone)

        val soundOnly = BreathingFeedbackPolicy.decide(BreathPhaseKind.EXHALE, RingerMode.NORMAL, FeedbackPreferences(vibration = false, sound = true))
        assertNull(soundOnly.vibrationPattern)
        assertTrue(soundOnly.playTone)

        val off = BreathingFeedbackPolicy.decide(BreathPhaseKind.EXHALE, RingerMode.NORMAL, FeedbackPreferences(vibration = false, sound = false))
        assertEquals(FeedbackDecision.NONE, off)
    }

    @Test
    fun vibrateMode_userDisabledVibration_nothing() {
        val d = BreathingFeedbackPolicy.decide(BreathPhaseKind.INHALE, RingerMode.VIBRATE, FeedbackPreferences(vibration = false, sound = true))
        assertEquals(FeedbackDecision.NONE, d)
    }

    @Test
    fun defaults_vibrationOn_soundOptionalOff() {
        assertTrue(FeedbackPreferences().vibration)
        assertFalse(FeedbackPreferences().sound)
    }

    private class FakeOutput(var mode: RingerMode) : FeedbackOutput {
        val vibrations = mutableListOf<List<Long>>()
        var tones = 0
        var releases = 0
        override fun ringerMode() = mode
        override fun vibrate(pattern: List<Long>) { vibrations += pattern }
        override fun playTone() { tones++ }
        override fun release() { releases++ }
    }

    @Test
    fun controller_appliesDecision_andReadsPreferencesOnEveryPhase() {
        val output = FakeOutput(RingerMode.NORMAL)
        var prefs = both
        val controller = BreathingFeedbackController(output) { prefs }

        controller.onPhase(BreathPhaseKind.INHALE)
        prefs = FeedbackPreferences(vibration = true, sound = false)
        controller.onPhase(BreathPhaseKind.HOLD)
        output.mode = RingerMode.SILENT
        val silent = controller.onPhase(BreathPhaseKind.EXHALE)

        assertEquals(listOf(BreathingFeedbackPolicy.INHALE_PATTERN, BreathingFeedbackPolicy.HOLD_PATTERN), output.vibrations)
        assertEquals(1, output.tones)
        assertEquals(FeedbackDecision.NONE, silent)
        controller.stop()
        assertEquals(1, output.releases)
    }

    @Test
    fun catalog_usesTypedPhases() {
        val box = Catalogs.BREATHING_EXERCISES.first { it.id == "box" }
        assertEquals(listOf(BreathPhaseKind.INHALE, BreathPhaseKind.HOLD, BreathPhaseKind.EXHALE, BreathPhaseKind.HOLD), box.phases.map { it.kind })
        assertTrue(box.phases.all { it.seconds == 4 })
        assertNotEquals(0, Catalogs.BREATHING_EXERCISES.first { it.id == "478" }.phases.sumOf { it.seconds })
    }
}
