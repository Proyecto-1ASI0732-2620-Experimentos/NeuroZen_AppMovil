package com.example.neurozen_front.domain.logic

import java.time.YearMonth

/** Client-side card checks (same rules as the backend). Card data is never stored on the device. */
object CardValidator {

    fun digitsOnly(value: String): String = value.filter { it.isDigit() }

    fun isValidNumber(number: String): Boolean {
        val digits = digitsOnly(number)
        if (digits.length !in 13..19 || digits.length != number.replace(" ", "").length) return false
        return luhn(digits)
    }

    fun luhn(digits: String): Boolean {
        var sum = 0
        var double = false
        for (i in digits.indices.reversed()) {
            var d = digits[i] - '0'
            if (double) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            double = !double
        }
        return digits.isNotEmpty() && sum % 10 == 0
    }

    /** Expiration in MM/YY format; valid through the last day of that month. */
    fun isValidExpiration(value: String, now: YearMonth = YearMonth.now()): Boolean {
        val match = Regex("""^(\d{2})/(\d{2})$""").matchEntire(value.trim()) ?: return false
        val month = match.groupValues[1].toInt()
        if (month !in 1..12) return false
        val year = 2000 + match.groupValues[2].toInt()
        return !YearMonth.of(year, month).isBefore(now)
    }

    fun isValidCvv(value: String): Boolean = Regex("""^\d{3,4}$""").matches(value)

    fun last4(number: String): String = digitsOnly(number).takeLast(4)
}
