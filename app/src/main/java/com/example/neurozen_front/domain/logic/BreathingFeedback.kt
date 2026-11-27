package com.example.neurozen_front.domain.logic

/** Kind of a breathing phase. The on-screen label comes from strings.xml. */
enum class BreathPhaseKind { INHALE, HOLD, EXHALE }

data class BreathPhase(val kind: BreathPhaseKind, val seconds: Int)

/** Phone sound profile, mapped from AudioManager.ringerMode by the Android adapter. */
enum class RingerMode { SILENT, VIBRATE, NORMAL }

/** User switches on the exercise screen (persisted). Sound is optional, so it starts off. */
data class FeedbackPreferences(val vibration: Boolean = true, val sound: Boolean = false)

/**
 * What to do when a phase starts. [vibrationPattern] uses the VibrationEffect.createWaveform timings format:
 * off/on durations in ms starting with an initial delay; null = do not vibrate.
 */
data class FeedbackDecision(val vibrationPattern: List<Long>?, val playTone: Boolean) {
    companion object {
        val NONE = FeedbackDecision(null, false)
    }
}

/**
 * Pure rules (no Android) for the haptic and sound cues of the guided breathing:
 * - inhale = one short pulse, hold = two short pulses, exhale = one long pulse;
 * - silent mode: neither sound nor vibration; vibrate mode: vibration only; normal: what the user enabled.
 */
object BreathingFeedbackPolicy {
    val INHALE_PATTERN: List<Long> = listOf(0L, 120L)
    val HOLD_PATTERN: List<Long> = listOf(0L, 60L, 90L, 60L)
    val EXHALE_PATTERN: List<Long> = listOf(0L, 450L)

    fun patternFor(kind: BreathPhaseKind): List<Long> = when (kind) {
        BreathPhaseKind.INHALE -> INHALE_PATTERN
        BreathPhaseKind.HOLD -> HOLD_PATTERN
        BreathPhaseKind.EXHALE -> EXHALE_PATTERN
    }

    fun decide(kind: BreathPhaseKind, ringer: RingerMode, prefs: FeedbackPreferences): FeedbackDecision {
        val canVibrate = prefs.vibration && ringer != RingerMode.SILENT
        val canSound = prefs.sound && ringer == RingerMode.NORMAL
        return FeedbackDecision(vibrationPattern = if (canVibrate) patternFor(kind) else null, playTone = canSound)
    }
}

/** Side effects of the cues, implemented with Vibrator/ToneGenerator on Android and with fakes in tests. */
interface FeedbackOutput {
    fun ringerMode(): RingerMode
    fun vibrate(pattern: List<Long>)
    fun playTone()

    /** Cancels the vibration and frees the sound resources (they are created again when needed). */
    fun release()
}

/** Applies [BreathingFeedbackPolicy] on every phase change; [preferences] is read at that moment. */
class BreathingFeedbackController(
    private val output: FeedbackOutput,
    private val preferences: () -> FeedbackPreferences
) {
    fun onPhase(kind: BreathPhaseKind): FeedbackDecision {
        val decision = BreathingFeedbackPolicy.decide(kind, output.ringerMode(), preferences())
        decision.vibrationPattern?.let(output::vibrate)
        if (decision.playTone) output.playTone()
        return decision
    }

    /** Call when the exercise is stopped, paused, finished or the screen is left. */
    fun stop() = output.release()
}
