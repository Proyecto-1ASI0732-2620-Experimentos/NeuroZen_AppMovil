package com.example.neurozen_front.domain

import com.example.neurozen_front.domain.logic.CardValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class CardValidatorTest {

    @Test
    fun luhn_validTestCards() {
        assertTrue(CardValidator.isValidNumber("4111111111111111"))
        assertTrue(CardValidator.isValidNumber("5555555555554444"))
        assertTrue(CardValidator.isValidNumber("4111 1111 1111 1111"))
    }

    @Test
    fun luhn_invalidNumbers() {
        assertFalse(CardValidator.isValidNumber("4111111111111112"))
        assertFalse(CardValidator.isValidNumber("1234"))
        assertFalse(CardValidator.isValidNumber("4111-1111-1111-111a"))
        assertFalse(CardValidator.isValidNumber(""))
    }

    @Test
    fun expiration_validUntilEndOfMonth() {
        val now = YearMonth.of(2026, 11)
        assertTrue(CardValidator.isValidExpiration("11/26", now))
        assertTrue(CardValidator.isValidExpiration("01/30", now))
        assertFalse(CardValidator.isValidExpiration("10/26", now))
        assertFalse(CardValidator.isValidExpiration("13/27", now))
        assertFalse(CardValidator.isValidExpiration("1/27", now))
    }

    @Test
    fun cvv_threeOrFourDigits() {
        assertTrue(CardValidator.isValidCvv("123"))
        assertTrue(CardValidator.isValidCvv("1234"))
        assertFalse(CardValidator.isValidCvv("12"))
        assertFalse(CardValidator.isValidCvv("12a"))
    }

    @Test
    fun last4() {
        assertEquals("1111", CardValidator.last4("4111 1111 1111 1111"))
    }
}
