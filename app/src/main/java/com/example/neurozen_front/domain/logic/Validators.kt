package com.example.neurozen_front.domain.logic

object Validators {
    private val EMAIL = Regex("""^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$""")
    private val LICENSE = Regex("""^\d{3,8}$""")
    private val HOUR = Regex("""^([01]\d|2[0-3]):[0-5]\d$""")

    fun isValidEmail(value: String): Boolean = EMAIL.matches(value.trim())

    /** At least 8 characters with letters and numbers. */
    fun isStrongPassword(value: String): Boolean =
        value.length >= 8 && value.any { it.isLetter() } && value.any { it.isDigit() }

    fun passwordError(value: String): String? = when {
        value.length < 8 -> "Mínimo 8 caracteres"
        value.none { it.isLetter() } || value.none { it.isDigit() } -> "Debe tener letras y números"
        else -> null
    }

    /** CPsP license number: digits only. */
    fun isValidLicense(value: String): Boolean = LICENSE.matches(value.trim())

    fun isValidHour(value: String): Boolean = HOUR.matches(value.trim())

    fun isValidStressLevel(value: Int): Boolean = value in 1..10

    fun isValidFullName(value: String): Boolean = value.trim().length >= 3 && value.trim().contains(' ')
}
