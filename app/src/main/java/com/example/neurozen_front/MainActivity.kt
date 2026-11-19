package com.example.neurozen_front

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import android.graphics.Color as AndroidColor
import com.example.neurozen_front.data.settings.ThemeSettingsStore
import com.example.neurozen_front.presentation.navigation.AppRoot
import com.example.neurozen_front.presentation.theme.NeuroZenTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themeSettings: ThemeSettingsStore

    /** Token from the password-reset link (neurozen://reset-password?token=… or the https App Link). */
    private val resetToken = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        resetToken.value = extractResetToken(intent?.data)
        setContent {
            val mode by themeSettings.mode.collectAsState()
            val dark = mode.isDark(isSystemInDarkTheme())
            // Iconos de la barra de estado y navegación según el tema elegido (no solo el del sistema)
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark }
                )
                onDispose {}
            }
            NeuroZenTheme(darkTheme = dark) {
                AppRoot(resetToken = resetToken.value, onResetTokenConsumed = { resetToken.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractResetToken(intent.data)?.let { resetToken.value = it }
    }

    private companion object {
        val LIGHT_SCRIM = AndroidColor.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = AndroidColor.argb(0x80, 0x1b, 0x1b, 0x1b)
    }

    private fun extractResetToken(uri: Uri?): String? {
        if (uri == null) return null
        val isReset = uri.host == "reset-password" || uri.path?.contains("reset-password") == true
        return if (isReset) uri.getQueryParameter("token")?.takeIf { it.isNotBlank() } else null
    }
}
