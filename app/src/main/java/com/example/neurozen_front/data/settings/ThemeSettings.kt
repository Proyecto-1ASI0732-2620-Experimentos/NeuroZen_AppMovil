package com.example.neurozen_front.data.settings

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Apariencia elegida por la persona. Por defecto se sigue la configuración del teléfono. */
enum class ThemeMode(val label: String, val description: String) {
    SYSTEM("Según el sistema", "Usa el modo claro u oscuro de tu teléfono"),
    LIGHT("Claro", "Fondo claro, ideal de día"),
    DARK("Oscuro", "Descansa la vista de noche");

    /** true si se debe pintar el tema oscuro. */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun from(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/** Preferencia no sensible: SharedPreferences propias, independientes de la sesión (se conserva al cerrar sesión). */
@Singleton
class ThemeSettingsStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("neurozen_appearance", Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(ThemeMode.from(prefs.getString(KEY, null)))
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun set(mode: ThemeMode) {
        prefs.edit { putString(KEY, mode.name) }
        _mode.value = mode
    }

    private companion object { const val KEY = "theme_mode" }
}
