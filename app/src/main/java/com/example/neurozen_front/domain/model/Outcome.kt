package com.example.neurozen_front.domain.model

/** Result of any operation that can fail. Error messages are ready to show in the UI (Spanish). */
sealed interface Outcome<out T> {
    data class Success<T>(val data: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}

sealed class AppError(open val message: String) {
    data class Network(override val message: String = "Sin conexión. Revisa tu internet e inténtalo de nuevo.") : AppError(message)
    data class Timeout(override val message: String = "El servidor está despertando; puede tardar hasta un minuto. Inténtalo de nuevo.") : AppError(message)
    data class Unauthorized(override val message: String = "Tu sesión expiró. Inicia sesión otra vez.") : AppError(message)
    data class Forbidden(override val message: String) : AppError(message)
    data class NotFound(override val message: String) : AppError(message)
    data class Conflict(override val message: String) : AppError(message)
    data class Validation(override val message: String, val fieldErrors: Map<String, List<String>> = emptyMap()) : AppError(message)
    data class Server(override val message: String) : AppError(message)
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(data))
    is Outcome.Failure -> this
}

fun <T> Outcome<T>.getOrNull(): T? = (this as? Outcome.Success)?.data

fun <T> Outcome<T>.errorOrNull(): AppError? = (this as? Outcome.Failure)?.error
