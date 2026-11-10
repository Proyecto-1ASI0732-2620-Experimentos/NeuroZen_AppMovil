package com.example.neurozen_front.domain.logic

import java.util.Base64

/** Reads the "exp" claim of a JWT without validating it (the server validates). */
object JwtDecoder {
    fun expirationEpochSeconds(token: String): Long? = runCatching {
        val parts = token.removePrefix("Bearer ").split(".")
        if (parts.size < 2) return null
        val json = String(Base64.getUrlDecoder().decode(parts[1].padEnd((parts[1].length + 3) / 4 * 4, '=')))
        Regex("\"exp\"\\s*:\\s*(\\d+)").find(json)?.groupValues?.get(1)?.toLong()
    }.getOrNull()
}
