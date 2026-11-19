package com.example.neurozen_front.presentation.psychologist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.neurozen_front.domain.logic.AvailabilityDraft
import com.example.neurozen_front.domain.logic.DateTimeUtils
import com.example.neurozen_front.presentation.components.NzTextField
import java.time.DayOfWeek

/** Days and hours of attention (used in Agenda, Perfil and the re-submission after a rejection). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvailabilityEditor(draft: AvailabilityDraft, onChange: (AvailabilityDraft) -> Unit, modifier: Modifier = Modifier, serverError: String? = null) {
    Column(modifier.testTag("availability_editor"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Días de atención", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DayOfWeek.entries.forEach { d ->
                FilterChip(
                    selected = d in draft.days, onClick = { onChange(draft.toggle(d)) }, label = { Text(DateTimeUtils.dayShort(d)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary),
                    modifier = Modifier.testTag("day_${d.name}")
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NzTextField(draft.start, { onChange(draft.copy(start = it.take(5))) }, "Desde (HH:mm)", Modifier.weight(1f).testTag("availability_start"), keyboardType = KeyboardType.Number)
            NzTextField(draft.end, { onChange(draft.copy(end = it.take(5))) }, "Hasta (HH:mm)", Modifier.weight(1f).testTag("availability_end"), keyboardType = KeyboardType.Number)
        }
        val error = draft.error ?: serverError
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("availability_error"))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(draft.summary(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
