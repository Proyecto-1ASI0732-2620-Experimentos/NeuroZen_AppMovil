package com.example.neurozen_front.data.session

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/** Adds "Authorization: Bearer <jwt>" and reports 401 on authenticated calls as an expired session. */
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
        return response
    }
}
