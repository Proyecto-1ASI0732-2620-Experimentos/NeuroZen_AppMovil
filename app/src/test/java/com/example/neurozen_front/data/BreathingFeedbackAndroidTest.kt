package com.example.neurozen_front.data

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.neurozen_front.data.feedback.AndroidFeedbackOutput
import com.example.neurozen_front.data.settings.BreathingFeedbackSettingsStore
import com.example.neurozen_front.domain.logic.FeedbackPreferences
import com.example.neurozen_front.domain.logic.RingerMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** A2: Android adapter (vibration, tone, ringer mode) and persisted switches, with mocked framework classes. */
class BreathingFeedbackAndroidTest {
    private val vibrator = mockk<Vibrator>(relaxed = true) { every { hasVibrator() } returns true }
    private val audio = mockk<AudioManager>()
    private val effect = mockk<VibrationEffect>()
    private val context = mockk<Context>()

    @Before fun setUp() {
        mockkStatic(VibrationEffect::class)
        every { VibrationEffect.createWaveform(any<LongArray>(), any<Int>()) } returns effect
        every { context.applicationContext } returns context
        every { context.getSystemService(Context.AUDIO_SERVICE) } returns audio
        every { context.getSystemService(Context.VIBRATOR_SERVICE) } returns vibrator
    }

    @After fun tearDown() = unmockkStatic(VibrationEffect::class)

    @Test
    fun ringerModes_areMapped_andUnknownAudioMakesNoSound() {
        val output = AndroidFeedbackOutput(context, sdkInt = 30) { null }
        every { audio.ringerMode } returns AudioManager.RINGER_MODE_SILENT
        assertEquals(RingerMode.SILENT, output.ringerMode())
        every { audio.ringerMode } returns AudioManager.RINGER_MODE_VIBRATE
        assertEquals(RingerMode.VIBRATE, output.ringerMode())
        every { audio.ringerMode } returns AudioManager.RINGER_MODE_NORMAL
        assertEquals(RingerMode.NORMAL, output.ringerMode())

        every { context.getSystemService(Context.AUDIO_SERVICE) } returns null
        assertEquals(RingerMode.VIBRATE, AndroidFeedbackOutput(context, sdkInt = 30) { null }.ringerMode())
    }

    @Test
    fun vibrate_usesWaveformWithoutRepeat_onlyWithVibrator() {
        val output = AndroidFeedbackOutput(context, sdkInt = 30) { null }
        output.vibrate(listOf(0L, 120L))
        verify { VibrationEffect.createWaveform(longArrayOf(0L, 120L), -1) }
        verify { vibrator.vibrate(effect) }

        output.vibrate(emptyList())
        every { vibrator.hasVibrator() } returns false
        output.vibrate(listOf(0L, 450L))
        verify(exactly = 1) { vibrator.vibrate(any<VibrationEffect>()) }
    }

    @Test
    fun api31_usesVibratorManager() {
        val manager = mockk<VibratorManager> { every { defaultVibrator } returns vibrator }
        every { context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) } returns manager
        AndroidFeedbackOutput(context, sdkInt = 31) { null }.vibrate(listOf(0L, 60L, 90L, 60L))
        verify { vibrator.vibrate(effect) }
        verify(exactly = 0) { context.getSystemService(Context.VIBRATOR_SERVICE) }
    }

    @Test
    fun tone_isCreatedLazily_reused_andReleased() {
        val tone = mockk<ToneGenerator>(relaxed = true)
        var created = 0
        val output = AndroidFeedbackOutput(context, sdkInt = 30) { created++; tone }
        output.playTone()
        output.playTone()
        assertEquals(1, created)
        verify(exactly = 2) { tone.startTone(ToneGenerator.TONE_PROP_BEEP, 150) }
        output.release()
        verify { tone.release() }
        verify { vibrator.cancel() }
        output.playTone()
        assertEquals(2, created)
    }

    @Test
    fun toneUnavailable_isSkipped_withoutVibrator_noCrash() {
        every { context.getSystemService(Context.VIBRATOR_SERVICE) } returns null
        val output = AndroidFeedbackOutput(context, sdkInt = 26) { null }
        output.playTone()
        output.vibrate(listOf(0L, 120L))
        output.release()
        verify(exactly = 0) { vibrator.vibrate(any<VibrationEffect>()) }
    }

    @Test
    fun settings_persistAndExposeTheSwitches() {
        val values = mutableMapOf<String, Boolean>()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { editor.putBoolean(any(), any()) } answers { values[firstArg()] = secondArg(); editor }
        val prefs = mockk<SharedPreferences> {
            every { getBoolean(any(), any()) } answers { values[firstArg()] ?: secondArg() }
            every { edit() } returns editor
        }
        val store = BreathingFeedbackSettingsStore(prefs)
        assertEquals(FeedbackPreferences(), store.preferences.value)
        store.setSound(true)
        store.setVibration(false)
        assertEquals(FeedbackPreferences(vibration = false, sound = true), store.preferences.value)
        assertFalse(values.getValue("vibration"))
        assertTrue(values.getValue("sound"))
        assertEquals(FeedbackPreferences(vibration = false, sound = true), BreathingFeedbackSettingsStore(prefs).preferences.value)
    }
}
