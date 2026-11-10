package com.example.neurozen_front.data.remote

import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.Outcome
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException

/** Maps HTTP status + backend ErrorResponse {message, errors} to a UI-ready AppError. */
object ErrorMapper {
    private val gson = Gson()

    fun fromHttp(code: Int, body: String?): AppError {
        val parsed = runCatching { gson.fromJson(body, ErrorResponseDto::class.java) }.getOrNull()
        val fieldErrors = parsed?.errors.orEmpty()
        val message = parsed?.message?.takeIf { it.isNotBlank() }
            ?: fieldErrors.values.flatten().firstOrNull()
        return when (code) {
            400, 422 -> AppError.Validation(message ?: "Revisa los datos ingresados.", fieldErrors)
            401 -> AppError.Unauthorized(message ?: "Correo o contraseña incorrectos.")
            403 -> AppError.Forbidden(message ?: "No tienes permiso para realizar esta acción.")
            404 -> AppError.NotFound(message ?: "No encontramos lo que buscabas.")
            409 -> AppError.Conflict(message ?: "La operación entra en conflicto con datos existentes.")
            in 500..599 -> AppError.Server(message ?: "El servidor tuvo un problema. Inténtalo más tarde.")
            else -> AppError.Server(message ?: "Error inesperado ($code).")
        }
    }

    fun fromThrowable(t: Throwable): AppError = when (t) {
        is SocketTimeoutException, is InterruptedIOException -> AppError.Timeout()
        is IOException -> AppError.Network()
        else -> AppError.Server("Error inesperado. Inténtalo de nuevo.")
    }
}

/** Calls that must return a body (200/201). */
suspend fun <T, R> apiCall(call: suspend () -> Response<T>, map: (T) -> R): Outcome<R> = try {
    val response = call()
    val body = response.body()
    if (response.isSuccessful && body != null) Outcome.Success(map(body))
    else if (response.isSuccessful) Outcome.Failure(AppError.Server("Respuesta vacía del servidor."))
    else Outcome.Failure(ErrorMapper.fromHttp(response.code(), response.errorBody()?.string()))
} catch (t: Throwable) {
    if (t is kotlinx.coroutines.CancellationException) throw t
    Outcome.Failure(ErrorMapper.fromThrowable(t))
}

/** Calls whose body is irrelevant (204, or 200 without schema). */
suspend fun <T> apiCallNoBody(call: suspend () -> Response<T>): Outcome<Unit> = try {
    val response = call()
    if (response.isSuccessful) Outcome.Success(Unit)
    else Outcome.Failure(ErrorMapper.fromHttp(response.code(), response.errorBody()?.string()))
} catch (t: Throwable) {
    if (t is kotlinx.coroutines.CancellationException) throw t
    Outcome.Failure(ErrorMapper.fromThrowable(t))
}
