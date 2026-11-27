package com.example.neurozen_front.data.feedback

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import com.example.neurozen_front.domain.logic.FeedbackOutput
import com.example.neurozen_front.domain.logic.RingerMode

/**
 * Vibration (VibratorManager on API 31+, Vibrator before; createWaveform exists from API 26 = minSdk)
 * and a short soft tone on the notification stream. Nothing here is logged.
 */
class AndroidFeedbackOutput(
    context: Context,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
    private val toneFactory: () -> ToneGenerator? = { createTone() }
) : FeedbackOutput {
    private val appContext = context.applicationContext
    private val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val vibrator: Vibrator? = resolveVibrator()
    private var tone: ToneGenerator? = null

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    private fun isAtLeastS(): Boolean = sdkInt >= Build.VERSION_CODES.S

    private fun resolveVibrator(): Vibrator? =
        if (isAtLeastS()) managerVibrator() else legacyVibrator()

    @RequiresApi(Build.VERSION_CODES.S)
    private fun managerVibrator(): Vibrator? =
        (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator

    /** Before API 31 the Vibrator service is the only way (deprecated there, still the official fallback). */
    @Suppress("DEPRECATION")
    private fun legacyVibrator(): Vibrator? = appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    override fun ringerMode(): RingerMode = when (audio?.ringerMode) {
        AudioManager.RINGER_MODE_SILENT -> RingerMode.SILENT
        AudioManager.RINGER_MODE_VIBRATE -> RingerMode.VIBRATE
        // Unknown audio service: the safest choice is to make no sound.
        null -> RingerMode.VIBRATE
        else -> RingerMode.NORMAL
    }

    override fun vibrate(pattern: List<Long>) {
        val v = vibrator ?: return
        if (!v.hasVibrator() || pattern.isEmpty()) return
        VibrationEffect.createWaveform(pattern.toLongArray(), NO_REPEAT)?.let(v::vibrate)
    }

    override fun playTone() {
        val generator = tone ?: toneFactory().also { tone = it } ?: return
        generator.startTone(ToneGenerator.TONE_PROP_BEEP, TONE_MS)
    }

    override fun release() {
        vibrator?.cancel()
        tone?.release()
        tone = null
    }

    companion object {
        private const val NO_REPEAT = -1
        private const val TONE_MS = 150
        private const val TONE_VOLUME = 35

        /** ToneGenerator throws RuntimeException when the audio system cannot create it: the cue is then skipped. */
        private fun createTone(): ToneGenerator? =
            runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, TONE_VOLUME) }.getOrNull()
    }
}
