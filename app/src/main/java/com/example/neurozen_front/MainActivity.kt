package com.example.neurozen_front

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.neurozen_front.presentation.navigation.AppRoot
import com.example.neurozen_front.presentation.theme.NeuroZenTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Token from the password-reset link (neurozen://reset-password?token=… or the https App Link). */
    private val resetToken = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        resetToken.value = extractResetToken(intent?.data)
        setContent {
            NeuroZenTheme {
                AppRoot(resetToken = resetToken.value, onResetTokenConsumed = { resetToken.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractResetToken(intent.data)?.let { resetToken.value = it }
    }

    private fun extractResetToken(uri: Uri?): String? {
        if (uri == null) return null
        val isReset = uri.host == "reset-password" || uri.path?.contains("reset-password") == true
        return if (isReset) uri.getQueryParameter("token")?.takeIf { it.isNotBlank() } else null
    }
}
