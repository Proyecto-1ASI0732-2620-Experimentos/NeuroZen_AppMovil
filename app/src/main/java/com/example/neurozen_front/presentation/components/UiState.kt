package com.example.neurozen_front.presentation.components

import com.example.neurozen_front.domain.model.Outcome

/** Loading / success / empty / error, used by every screen that loads data. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun <T> Outcome<T>.toUiState(isEmpty: (T) -> Boolean = { (it as? Collection<*>)?.isEmpty() == true }): UiState<T> = when (this) {
    is Outcome.Success -> if (isEmpty(data)) UiState.Empty else UiState.Success(data)
    is Outcome.Failure -> UiState.Error(error.message)
}
