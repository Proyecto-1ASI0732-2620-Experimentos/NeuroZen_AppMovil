package com.example.neurozen_front.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.neurozen_front.domain.logic.FeedbackPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Vibration / sound switches of the guided breathing. Non-sensitive: plain SharedPreferences, like the appearance setting. */
@Singleton
class BreathingFeedbackSettingsStore internal constructor(private val prefs: SharedPreferences) {

    @Inject constructor(@ApplicationContext context: Context) :
        this(context.getSharedPreferences(FILE, Context.MODE_PRIVATE))

    private val _preferences = MutableStateFlow(read())
    val preferences: StateFlow<FeedbackPreferences> = _preferences.asStateFlow()

    private fun read() = FeedbackPreferences(
        vibration = prefs.getBoolean(KEY_VIBRATION, FeedbackPreferences().vibration),
        sound = prefs.getBoolean(KEY_SOUND, FeedbackPreferences().sound)
    )

    fun setVibration(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_VIBRATION, enabled) }
        _preferences.value = _preferences.value.copy(vibration = enabled)
    }

    fun setSound(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SOUND, enabled) }
        _preferences.value = _preferences.value.copy(sound = enabled)
    }

    private companion object {
        const val FILE = "neurozen_breathing"
        const val KEY_VIBRATION = "vibration"
        const val KEY_SOUND = "sound"
    }
}
