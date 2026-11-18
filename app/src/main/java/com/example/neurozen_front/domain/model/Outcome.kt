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
    data class TooManyRequests(override val message: String = "Demasiados intentos. Espera unos segundos e inténtalo de nuevo.") : AppError(message)
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(data))
    is Outcome.Failure -> this
}

fun <T> Outcome<T>.getOrNull(): T? = (this as? Outcome.Success)?.data

fun <T> Outcome<T>.errorOrNull(): AppError? = (this as? Outcome.Failure)?.error

/** Message of the backend for a psychologist whose profile is PENDING or REJECTED (403). */
private val NOT_VERIFIED = Regex("no est[aá] verificad", RegexOption.IGNORE_CASE)

/** Message of the backend for a library resource above the user's plan (403). */
private val PLAN_REQUIRED = Regex("disponible desde el plan", RegexOption.IGNORE_CASE)

/** 403 «Tu cuenta profesional aún no está verificada»: the psychologist went back to review. */
fun AppError.isProfessionalNotVerified(): Boolean = this is AppError.Forbidden && NOT_VERIFIED.containsMatchIn(message)

/** 403 «Este recurso está disponible desde el plan Avanzado»: resource outside the user's plan. */
fun AppError.isPlanRequired(): Boolean = this is AppError.Forbidden && PLAN_REQUIRED.containsMatchIn(message)

fun isProfessionalNotVerifiedMessage(message: String?): Boolean = message != null && NOT_VERIFIED.containsMatchIn(message)
