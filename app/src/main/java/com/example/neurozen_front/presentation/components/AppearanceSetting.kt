package com.example.neurozen_front.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.example.neurozen_front.data.settings.ThemeMode
import com.example.neurozen_front.data.settings.ThemeSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(private val store: ThemeSettingsStore) : ViewModel() {
    val mode = store.mode
    fun select(mode: ThemeMode) = store.set(mode)
}

fun ThemeMode.icon(): ImageVector = when (this) {
    ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
    ThemeMode.LIGHT -> Icons.Outlined.LightMode
    ThemeMode.DARK -> Icons.Outlined.DarkMode
}

/** Fila «Tema» para los grupos de ajustes del perfil; abre la elección Sistema / Claro / Oscuro. */
@Composable
fun AppearanceRow(vm: AppearanceViewModel = hiltViewModel()) {
    val mode by vm.mode.collectAsState()
    var open by remember { mutableStateOf(false) }
    Column(Modifier.testTag("appearance_row")) {
        ListRow(mode.icon(), "Tema", "Modo claro, oscuro o el del sistema", trailingText = mode.label) { open = true }
    }
    if (open) {
        ThemeModeDialog(selected = mode, onSelect = { vm.select(it); open = false }, onDismiss = { open = false })
    }
}

@Composable
fun ThemeModeDialog(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tema de la app") },
        text = {
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(selected = option == selected, role = Role.RadioButton, onClick = { onSelect(option) })
                            .padding(vertical = 10.dp)
                            .testTag("theme_${option.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(option.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(option.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
