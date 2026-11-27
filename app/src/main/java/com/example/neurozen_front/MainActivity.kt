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
import com.example.neurozen_front.domain.logic.ResetLink
import com.example.neurozen_front.domain.logic.ResetLinkResult
import com.example.neurozen_front.presentation.navigation.AppRoot
import com.example.neurozen_front.presentation.theme.NeuroZenTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themeSettings: ThemeSettingsStore

    /** Password-reset link (https App Link or neurozen://reset-password). null = no link pending. Never logged. */
    private val resetLink = mutableStateOf<ResetLinkResult?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        resetLink.value = classifyResetLink(intent?.data)
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
                AppRoot(resetLink = resetLink.value, onResetLinkConsumed = { resetLink.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // App already open (singleTop): the new link replaces the pending one.
        classifyResetLink(intent.data)?.let { resetLink.value = it }
    }

    private companion object {
        val LIGHT_SCRIM = AndroidColor.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = AndroidColor.argb(0x80, 0x1b, 0x1b, 0x1b)
    }

    /** Only reset links of the configured web host (or the dev scheme) are accepted, and the token is validated. */
    private fun classifyResetLink(uri: Uri?): ResetLinkResult? {
        if (uri == null) return null
        val result = ResetLink.classify(
            uri.scheme, uri.host, uri.path, runCatching { uri.getQueryParameter("token") }.getOrNull(), BuildConfig.RESET_LINK_HOST
        )
        return result.takeUnless { it == ResetLinkResult.NotResetLink }
    }
}
