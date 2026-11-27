package com.example.neurozen_front.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.example.neurozen_front.R
import com.example.neurozen_front.presentation.theme.NzColors
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

/** Live checklist with exactly the backend password policy (see [Validators.passwordChecklist]). */
@Composable
fun PasswordRequirements(password: String, modifier: Modifier = Modifier) {
    val check = Validators.passwordChecklist(password)
    Column(modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        PasswordRule(R.string.password_rule_length, check.minLength)
        PasswordRule(R.string.password_rule_letter, check.hasLetter)
        PasswordRule(R.string.password_rule_digit, check.hasDigit)
        PasswordRule(R.string.password_rule_max_bytes, check.withinMaxBytes)
    }
}

@Composable
private fun PasswordRule(@StringRes label: Int, ok: Boolean) {
    val text = stringResource(label)
    val state = stringResource(if (ok) R.string.password_rule_met else R.string.password_rule_pending)
    val description = stringResource(R.string.password_rule_a11y, text, state)
    Row(
        Modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            if (ok) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked, contentDescription = null,
            tint = if (ok) NzColors.Low else MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp)
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
