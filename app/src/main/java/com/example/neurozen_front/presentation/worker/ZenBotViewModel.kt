package com.example.neurozen_front.presentation.worker

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neurozen_front.R
import com.example.neurozen_front.domain.logic.BotSuggestion
import com.example.neurozen_front.domain.logic.ZenBotEngine
import com.example.neurozen_front.domain.logic.ZenBotResources
import com.example.neurozen_front.domain.model.LibraryResource
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** [text] comes from the engine or the user; [textRes] is a local message from strings.xml. */
data class ChatMessage(
    val text: String,
    val fromUser: Boolean,
    val isCrisis: Boolean = false,
    val suggestion: BotSuggestion? = null,
    val resources: List<LibraryResource> = emptyList(),
    @param:StringRes val textRes: Int? = null
)

/** Initial options (the label is also the text sent, so the same rules and the crisis check apply). */
data class QuickOption(@param:StringRes val label: Int)

data class ZenBotUi(
    val messages: List<ChatMessage> = listOf(ChatMessage(ZenBotEngine.GREETING, false)),
    val input: String = "",
    /** «ZenBot está escribiendo…» while real resources are looked up (never for crisis replies). */
    val typing: Boolean = false
) {
    val showQuickOptions: Boolean get() = messages.none { it.fromUser }
}

/**
 * Local rules: what the user types is only sent to the backend as a short topic query of the library search
 * (e.g. «respiración»), never the message itself. The crisis reply is computed synchronously and shown at once.
 */
@HiltViewModel
class ZenBotViewModel @Inject constructor(private val library: LibraryRepository) : ViewModel() {
    private val _ui = MutableStateFlow(ZenBotUi())
    val ui = _ui.asStateFlow()

    fun onInput(v: String) = _ui.update { it.copy(input = v.take(500)) }

    fun send() {
        val text = _ui.value.input.trim()
        if (text.isEmpty()) return
        _ui.update { it.copy(input = "") }
        respond(text)
    }

    /** A quick option behaves exactly like typing its text. */
    fun sendQuick(text: String) {
        if (text.isNotBlank()) respond(text.trim())
    }

    private fun respond(text: String) {
        val reply = ZenBotEngine.reply(text)
        _ui.update {
            it.copy(messages = it.messages + ChatMessage(text, true) + ChatMessage(reply.text, false, reply.isCrisis, reply.suggestion))
        }
        val query = ZenBotResources.queryFor(text, reply) ?: return
        _ui.update { it.copy(typing = true) }
        viewModelScope.launch {
            val found = findResources(query)
            val message = when {
                found == null -> ChatMessage("", false, textRes = R.string.zenbot_resources_error)
                found.isEmpty() -> ChatMessage("", false, textRes = R.string.zenbot_resources_none)
                else -> ChatMessage("", false, resources = found, textRes = R.string.zenbot_resources_found)
            }
            _ui.update { it.copy(typing = false, messages = it.messages + message) }
        }
    }

    /** /search first; if it fails, the plan-filtered list filtered locally; null when the library cannot be read. */
    private suspend fun findResources(query: String): List<LibraryResource>? =
        when (val r = library.search(query, ZenBotResources.MAX_RESULTS)) {
            is Outcome.Success -> r.data.take(ZenBotResources.MAX_RESULTS)
            is Outcome.Failure -> when (val all = library.getAll()) {
                is Outcome.Success -> ZenBotResources.filterLocally(all.data, query)
                is Outcome.Failure -> null
            }
        }

    companion object {
        val QUICK_OPTIONS = listOf(
            QuickOption(R.string.zenbot_option_stressed),
            QuickOption(R.string.zenbot_option_breathe),
            QuickOption(R.string.zenbot_option_sleep),
            QuickOption(R.string.zenbot_option_test),
            QuickOption(R.string.zenbot_option_record),
            QuickOption(R.string.zenbot_option_psychologist),
            QuickOption(R.string.zenbot_option_book),
            QuickOption(R.string.zenbot_option_my_appointments),
            QuickOption(R.string.zenbot_option_breaks),
            QuickOption(R.string.zenbot_option_video)
        )
    }
}
