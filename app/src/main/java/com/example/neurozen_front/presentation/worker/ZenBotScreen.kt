package com.example.neurozen_front.presentation.worker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.example.neurozen_front.domain.logic.BotSuggestion
import com.example.neurozen_front.domain.logic.ZenBotEngine
import com.example.neurozen_front.presentation.components.NzTopBar
import com.example.neurozen_front.presentation.theme.NzColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ChatMessage(val text: String, val fromUser: Boolean, val isCrisis: Boolean = false, val suggestion: BotSuggestion? = null)

data class ZenBotUi(val messages: List<ChatMessage> = listOf(ChatMessage(ZenBotEngine.GREETING, false)), val input: String = "")

/** Local rules only: nothing typed here leaves the phone. */
@HiltViewModel
class ZenBotViewModel @Inject constructor() : ViewModel() {
    private val _ui = MutableStateFlow(ZenBotUi())
    val ui = _ui.asStateFlow()

    fun onInput(v: String) = _ui.update { it.copy(input = v.take(500)) }

    fun send() {
        val text = _ui.value.input.trim()
        if (text.isEmpty()) return
        val reply = ZenBotEngine.reply(text)
        _ui.update {
            it.copy(input = "", messages = it.messages + ChatMessage(text, true) + ChatMessage(reply.text, false, reply.isCrisis, reply.suggestion))
        }
    }
}

@Composable
fun ZenBotScreen(onBack: () -> Unit, onSuggestion: (BotSuggestion) -> Unit, vm: ZenBotViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val listState = rememberLazyListState()
    val uri = LocalUriHandler.current
    LaunchedEffect(ui.messages.size) { listState.animateScrollToItem(ui.messages.lastIndex) }
    Scaffold(topBar = { NzTopBar("ZenBot", onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().imePadding()) {
            LazyColumn(Modifier.weight(1f), state = listState, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ui.messages) { m ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = if (m.fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
                        Column(
                            Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(16.dp))
                                .background(
                                    when {
                                        m.isCrisis -> NzColors.High.copy(alpha = 0.15f)
                                        m.fromUser -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ).padding(12.dp)
                                .then(if (m.isCrisis) Modifier.testTag("crisis_message") else Modifier)
                        ) {
                            Text(m.text, color = if (m.fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            if (m.isCrisis) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AssistChip(onClick = { uri.openUri("tel:113") }, label = { Text("Llamar 113") })
                                    AssistChip(onClick = { uri.openUri("tel:106") }, label = { Text("Llamar 106") })
                                }
                            }
                            m.suggestion?.let { s ->
                                AssistChip(onClick = { onSuggestion(s) }, label = { Text(suggestionLabel(s)) })
                            }
                        }
                    }
                }
            }
            Text(
                "ZenBot da sugerencias generales y no reemplaza a un profesional.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(ui.input, vm::onInput, Modifier.weight(1f).testTag("zenbot_input"), placeholder = { Text("Escribe cómo te sientes…") }, shape = RoundedCornerShape(20.dp))
                IconButton(onClick = vm::send, modifier = Modifier.testTag("zenbot_send")) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar") }
            }
        }
    }
}

private fun suggestionLabel(s: BotSuggestion) = when (s) {
    BotSuggestion.BREATHING -> "Ir a respiración"
    BotSuggestion.TRIGGERS -> "Registrar estrés"
    BotSuggestion.ASSESSMENT -> "Hacer el test"
    BotSuggestion.PROFESSIONALS -> "Ver psicólogos"
    BotSuggestion.BREAKS -> "Programar pausas"
    BotSuggestion.LIBRARY -> "Ver biblioteca"
}
