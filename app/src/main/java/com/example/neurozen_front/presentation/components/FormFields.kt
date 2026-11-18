package com.example.neurozen_front.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.neurozen_front.domain.logic.Catalogs
import com.example.neurozen_front.domain.logic.Validators

/**
 * Numeric-only field (DNI, CPsP license): number pad, keeps only ASCII digits 0-9 (also when pasting) and limits the
 * length. Kotlin's Char.isDigit() also accepts digits of other alphabets, which the backend rejects.
 */
@Composable
fun DigitsField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    maxLength: Int,
    modifier: Modifier = Modifier,
    error: String? = null,
    supporting: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(Validators.digitsOnly(it, maxLength)) },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = nzFieldColors()
    )
}

/** Closed catalog of specialties (the backend only accepts these values); shows the Spanish label. */
@Composable
fun SpecialtyField(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier, error: String? = null) {
    var expanded by remember { mutableStateOf(false) }
    val label = Catalogs.SPECIALTIES.firstOrNull { it.key == selected }?.label.orEmpty()
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Especialidad") },
            placeholder = { Text("Selecciona tu especialidad") },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = nzFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        // Transparent layer so the whole field opens the menu (a read-only text field does not get clicks).
        Box(
            Modifier.matchParentSize().testTag("specialty_field")
                .semantics { role = Role.DropdownList; contentDescription = "Especialidad: ${label.ifBlank { "sin seleccionar" }}" }
                .clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Catalogs.SPECIALTIES.forEach { s ->
                DropdownMenuItem(text = { Text(s.label) }, onClick = { onSelect(s.key); expanded = false })
            }
        }
    }
}
