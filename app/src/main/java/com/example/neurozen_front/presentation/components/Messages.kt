package com.example.neurozen_front.presentation.components

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** One-shot messages from a ViewModel to a Snackbar. */
class MessageChannel {
    private val channel = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = channel.receiveAsFlow()
    fun send(message: String) {
        channel.trySend(message)
    }
}
