package com.example.neurozen_front.presentation.worker

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.neurozen_front.R
import com.example.neurozen_front.domain.logic.BotSuggestion
import com.example.neurozen_front.domain.model.LibraryResource
import com.example.neurozen_front.presentation.components.NzTopBar
import com.example.neurozen_front.presentation.theme.NzColors

@Composable
fun ZenBotScreen(
    onBack: () -> Unit,
    onSuggestion: (BotSuggestion) -> Unit,
    vm: ZenBotViewModel = hiltViewModel(),
    onOpenResource: (Long) -> Unit = {}
) {
    val ui by vm.ui.collectAsState()
    val listState = rememberLazyListState()
    val uri = LocalUriHandler.current
    val lastIndex = ui.messages.lastIndex + (if (ui.typing) 1 else 0) + (if (ui.showQuickOptions) 1 else 0)
    LaunchedEffect(ui.messages.size, ui.typing) { listState.animateScrollToItem(lastIndex.coerceAtLeast(0)) }
    Scaffold(topBar = { NzTopBar(stringResource(R.string.zenbot_name), onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().imePadding()) {
            LazyColumn(Modifier.weight(1f), state = listState, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(ui.messages) { index, m ->
                    // The newest bot message is announced by TalkBack (polite live region).
                    val announce = !m.fromUser && index == ui.messages.lastIndex && index > 0
                    Box(
                        Modifier.fillMaxWidth().then(if (announce) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier),
                        contentAlignment = if (m.fromUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Column(
                            Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(16.dp))
                                .background(
                                    when {
                                        m.isCrisis -> NzColors.High.copy(alpha = 0.15f)
                                        m.fromUser -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ).padding(12.dp)
                                .then(if (m.isCrisis) Modifier.testTag("crisis_message") else Modifier),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val text = m.textRes?.let { stringResource(it) } ?: m.text
                            Text(text, color = if (m.fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            if (m.isCrisis) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AssistChip(onClick = { uri.openUri("tel:113") }, label = { Text("Llamar 113") })
                                    AssistChip(onClick = { uri.openUri("tel:106") }, label = { Text("Llamar 106") })
                                }
                            }
                            m.resources.forEach { r -> ResourceRecommendationCard(r) { onOpenResource(r.id) } }
                            m.suggestion?.let { s ->
                                AssistChip(onClick = { onSuggestion(s) }, label = { Text(stringResource(suggestionLabel(s))) })
                            }
                        }
                    }
                }
                if (ui.showQuickOptions) {
                    item { QuickOptions { vm.sendQuick(it) } }
                }
                if (ui.typing) {
                    item {
                        Text(
                            stringResource(R.string.zenbot_typing),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp).semantics { liveRegion = LiveRegionMode.Polite }.testTag("zenbot_typing")
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.zenbot_disclaimer),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    ui.input, vm::onInput, Modifier.weight(1f).testTag("zenbot_input"),
                    placeholder = { Text(stringResource(R.string.zenbot_placeholder)) }, shape = RoundedCornerShape(20.dp)
                )
                IconButton(onClick = vm::send, modifier = Modifier.testTag("zenbot_send")) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.zenbot_send))
                }
            }
        }
    }
}

/** The 10 initial options of the web; each one is sent as if the user had typed it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickOptions(onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.zenbot_options_title), style = MaterialTheme.typography.labelLarge, modifier = Modifier.semantics { heading() })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenBotViewModel.QUICK_OPTIONS.forEach { option ->
                val label = stringResource(option.label)
                SuggestionChip(onClick = { onSelect(label) }, label = { Text(label) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
    }
}

/** Real resource of the library (plan-filtered): thumbnail, title, duration, category and «Ver video». */
@Composable
private fun ResourceRecommendationCard(r: LibraryResource, onOpen: () -> Unit) {
    val kind = ResourceKind.of(r.type)
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            r.thumbnail?.let {
                AsyncImage(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            }
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(r.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.zenbot_resource_meta, kind.label, durationLabel(r)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onOpen, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(if (kind == ResourceKind.VIDEO) R.string.zenbot_watch_video else R.string.zenbot_open_resource))
                }
            }
        }
    }
}

private fun suggestionLabel(s: BotSuggestion): Int = when (s) {
    BotSuggestion.BREATHING -> R.string.zenbot_go_breathing
    BotSuggestion.TRIGGERS -> R.string.zenbot_go_triggers
    BotSuggestion.ASSESSMENT -> R.string.zenbot_go_assessment
    BotSuggestion.PROFESSIONALS -> R.string.zenbot_go_professionals
    BotSuggestion.BREAKS -> R.string.zenbot_go_breaks
    BotSuggestion.LIBRARY -> R.string.zenbot_go_library
    BotSuggestion.MY_APPOINTMENTS -> R.string.zenbot_go_appointments
}
