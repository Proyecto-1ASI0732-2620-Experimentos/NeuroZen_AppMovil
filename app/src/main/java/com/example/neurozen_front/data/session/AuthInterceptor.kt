package com.example.neurozen_front.data.session

import com.example.neurozen_front.domain.model.isProfessionalNotVerifiedMessage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds "Authorization: Bearer <jwt>" and reports 401 on authenticated calls as an expired session.
 * A 403 «Tu cuenta profesional aún no está verificada» (psychologist back to PENDING after a name or license change)
 * is reported too, so the app returns to the verification status screen from any screen.
 */
@Singleton
class AuthInterceptor @Inject constructor(private val sessions: SessionRepositoryImpl) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val isPublic = original.url.encodedPath.contains("/authentication/")
        val token = sessions.currentToken()
        val request = if (!isPublic && token != null) {
            original.newBuilder().header("Authorization", "Bearer $token").build()
        } else original
        val response = chain.proceed(request)
        if (response.code == 401 && !isPublic && token != null) sessions.notifyExpired()
        if (response.code == 403 && !isPublic && token != null) {
            // peekBody does not consume the body: the repository still reads it for the error message.
            val body = runCatching { response.peekBody(MAX_PEEK_BYTES).string() }.getOrNull()
            if (isProfessionalNotVerifiedMessage(body)) sessions.notifyProfessionalNotVerified()
        }
        return response
    }

    private companion object {
        const val MAX_PEEK_BYTES = 4096L
    }
}
