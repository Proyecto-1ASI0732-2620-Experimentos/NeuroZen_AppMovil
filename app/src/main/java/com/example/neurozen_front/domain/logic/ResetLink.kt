package com.example.neurozen_front.domain.logic

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** Result of an incoming deep link. */
sealed interface ResetLinkResult {
    /** Not a password-reset link: the app opens normally. */
    data object NotResetLink : ResetLinkResult
    /** A reset link whose token is missing or malformed: the reset screen explains it and offers a new link. */
    data object InvalidToken : ResetLinkResult
    data class Valid(val token: String) : ResetLinkResult
}

/**
 * Extraction and validation of the password-reset token. Pure Kotlin (no android.net.Uri) so it is unit tested.
 *
 * Accepted links:
 * - App Link: https://<webHost>/reset-password?token=… (the same URL the web already handles);
 * - development fallback: neurozen://reset-password?token=….
 *
 * The token is never logged. It must look like a real token: 16-512 characters of
 * ASCII letters, digits and the symbols - _ . ~ + / = (hex, GUID, base64, base64url or JWT-like values).
 * Spaces, quotes, angle brackets and any other character are rejected.
 */
object ResetLink {
    const val PATH_PREFIX = "/reset-password"
    const val CUSTOM_SCHEME = "neurozen"
    const val CUSTOM_HOST = "reset-password"
    const val MIN_TOKEN_LENGTH = 16
    const val MAX_TOKEN_LENGTH = 512
    private const val TOKEN_PARAM = "token"
    private const val MAX_LINK_LENGTH = 2048
    private const val TOKEN_SYMBOLS = "-_.~+/="

    /** Plain character check (no regular expression). */
    private fun isTokenChar(c: Char): Boolean =
        c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c in TOKEN_SYMBOLS

    fun isValidToken(token: String?): Boolean =
        token != null && token.length in MIN_TOKEN_LENGTH..MAX_TOKEN_LENGTH && token.all(::isTokenChar)

    /** true when the parts belong to one of the accepted reset links. */
    fun isResetLink(scheme: String?, host: String?, path: String?, webHost: String): Boolean = when {
        scheme.equals("https", ignoreCase = true) ->
            host.equals(webHost, ignoreCase = true) && path != null && (path == PATH_PREFIX || path.startsWith("$PATH_PREFIX/"))
        scheme.equals(CUSTOM_SCHEME, ignoreCase = true) -> host.equals(CUSTOM_HOST, ignoreCase = true)
        else -> false
    }

    fun classify(scheme: String?, host: String?, path: String?, tokenParam: String?, webHost: String): ResetLinkResult {
        if (!isResetLink(scheme, host, path, webHost)) return ResetLinkResult.NotResetLink
        val token = tokenParam?.trim()
        return if (isValidToken(token)) ResetLinkResult.Valid(token.orEmpty()) else ResetLinkResult.InvalidToken
    }

    /**
     * Token of an incoming deep link, or null when the link is not a reset link or the token is not valid.
     * [tokenParam] is the already decoded query value (Uri.getQueryParameter).
     */
    fun tokenFromLink(scheme: String?, host: String?, path: String?, tokenParam: String?, webHost: String): String? {
        return (classify(scheme, host, path, tokenParam, webHost) as? ResetLinkResult.Valid)?.token
    }

    /**
     * What the user pasted in the manual field: either the whole link of the e-mail or only the token.
     * Returns the valid token or null.
     */
    fun tokenFromUserInput(input: String, webHost: String): String? {
        val text = input.trim()
        if (text.isEmpty() || text.length > MAX_LINK_LENGTH) return null
        if (isValidToken(text)) return text
        val uri = runCatching { URI(text) }.getOrNull() ?: return null
        val token = queryParameter(uri.rawQuery, TOKEN_PARAM)
        return tokenFromLink(uri.scheme, uri.host, uri.path, token, webHost)
    }

    private fun queryParameter(rawQuery: String?, name: String): String? =
        rawQuery?.split('&')
            ?.map { it.split('=', limit = 2) }
            ?.firstOrNull { it.size == 2 && it[0] == name }
            ?.let { runCatching { URLDecoder.decode(it[1], StandardCharsets.UTF_8.name()) }.getOrNull() }
}
