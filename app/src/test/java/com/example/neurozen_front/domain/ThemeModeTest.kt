package com.example.neurozen_front.domain

import android.content.Context
import android.content.SharedPreferences
import com.example.neurozen_front.data.settings.ThemeMode
import com.example.neurozen_front.data.settings.ThemeSettingsStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test
    fun system_followsThePhone_lightAndDarkAreFixed() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemDark = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemDark = false))
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = true))
        assertTrue(ThemeMode.DARK.isDark(systemDark = false))
    }

    @Test
    fun unknownOrMissingValue_fallsBackToSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.from(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.from("SEPIA"))
        assertEquals(ThemeMode.DARK, ThemeMode.from("DARK"))
        assertEquals(listOf("Según el sistema", "Claro", "Oscuro"), ThemeMode.entries.map { it.label })
    }

    @Test
    fun store_readsSavedMode_andPersistsChanges() {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { editor.putString(any(), any()) } returns editor
        val prefs = mockk<SharedPreferences> {
            every { getString("theme_mode", null) } returns "LIGHT"
            every { edit() } returns editor
        }
        val context = mockk<Context> { every { getSharedPreferences("neurozen_appearance", Context.MODE_PRIVATE) } returns prefs }
        val store = ThemeSettingsStore(context)
        assertEquals(ThemeMode.LIGHT, store.mode.value)
        store.set(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, store.mode.value)
        verify { editor.putString("theme_mode", "DARK") }
        verify { editor.apply() }
    }
}
