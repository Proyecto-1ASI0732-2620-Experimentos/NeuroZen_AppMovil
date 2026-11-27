package com.example.neurozen_front.domain

import com.example.neurozen_front.domain.logic.ResetLink
import com.example.neurozen_front.domain.logic.ResetLinkResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A3: extraction and validation of the token of the password-reset link. */
class ResetLinkTest {
    private val host = "neurozen-web-frontend.vercel.app"
    private val token = "AbC123-_.~xyz789AbC123"

    @Test
    fun appLink_validToken() {
        assertEquals(ResetLinkResult.Valid(token), ResetLink.classify("https", host, "/reset-password", token, host))
        assertEquals(token, ResetLink.tokenFromLink("HTTPS", host.uppercase(), "/reset-password/", " $token ", host))
    }

    @Test
    fun customScheme_validToken() {
        assertEquals(ResetLinkResult.Valid(token), ResetLink.classify("neurozen", "reset-password", null, token, host))
    }

    @Test
    fun otherHostsOrPaths_areNotResetLinks() {
        assertEquals(ResetLinkResult.NotResetLink, ResetLink.classify("https", "evil.example.com", "/reset-password", token, host))
        assertEquals(ResetLinkResult.NotResetLink, ResetLink.classify("https", host, "/other/reset-password", token, host))
        assertEquals(ResetLinkResult.NotResetLink, ResetLink.classify("https", host, "/reset-passwordX", token, host))
        assertEquals(ResetLinkResult.NotResetLink, ResetLink.classify("http", host, "/reset-password", token, host))
        assertEquals(ResetLinkResult.NotResetLink, ResetLink.classify("neurozen", "other", null, token, host))
        assertEquals(ResetLinkResult.NotResetLink, ResetLink.classify(null, null, null, token, host))
        assertEquals(ResetLinkResult.NotResetLink, ResetLink.classify("https", host, null, token, host))
    }

    @Test
    fun resetLink_withMissingOrMalformedToken_isInvalid() {
        listOf(null, "", "   ", "short", "has space inside token 123", "<script>alert(1)</script>xx", "a".repeat(513), "tokén-con-tilde-12345")
            .forEach { assertEquals(it, ResetLinkResult.InvalidToken, ResetLink.classify("https", host, "/reset-password", it, host)) }
        assertNull(ResetLink.tokenFromLink("https", host, "/reset-password", "short", host))
    }

    @Test
    fun tokenValidation_limitsAndCharacters() {
        assertTrue(ResetLink.isValidToken("a".repeat(ResetLink.MIN_TOKEN_LENGTH)))
        assertTrue(ResetLink.isValidToken("a".repeat(ResetLink.MAX_TOKEN_LENGTH)))
        assertFalse(ResetLink.isValidToken("a".repeat(ResetLink.MIN_TOKEN_LENGTH - 1)))
        assertTrue(ResetLink.isValidToken("abc+def/ghi=jkl0123=="))
        assertFalse(ResetLink.isValidToken(null))
    }

    @Test
    fun userInput_acceptsTheWholeLinkOrOnlyTheToken() {
        assertEquals(token, ResetLink.tokenFromUserInput("  $token  ", host))
        assertEquals(token, ResetLink.tokenFromUserInput("https://$host/reset-password?token=$token", host))
        assertEquals(token, ResetLink.tokenFromUserInput("https://$host/reset-password?lang=es&token=$token", host))
        assertEquals("abc+def/ghi=jkl0123==", ResetLink.tokenFromUserInput("https://$host/reset-password?token=abc%2Bdef%2Fghi%3Djkl0123%3D%3D", host))
    }

    @Test
    fun userInput_rejectsOtherLinksAndGarbage() {
        assertNull(ResetLink.tokenFromUserInput("", host))
        assertNull(ResetLink.tokenFromUserInput("https://evil.example.com/reset-password?token=$token", host))
        assertNull(ResetLink.tokenFromUserInput("https://$host/reset-password", host))
        assertNull(ResetLink.tokenFromUserInput("https://$host/reset-password?token", host))
        assertNull(ResetLink.tokenFromUserInput("no es un enlace ni un token", host))
        assertNull(ResetLink.tokenFromUserInput("https://$host/reset-password?token=%zz", host))
        assertNull(ResetLink.tokenFromUserInput("x".repeat(3000), host))
    }
}
