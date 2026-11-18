package com.example.neurozen_front.domain.logic

/**
 * Form rules aligned with the backend (SignUpResource / PersonName / Dni / PasswordPolicy / Professional).
 * The backend is the final authority; these only report the error earlier to the user.
 */
object Validators {
    private val EMAIL = Regex("""^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$""")

    // [0-9] (ASCII) on purpose: Kotlin's \d and Char.isDigit() also accept digits of other alphabets,
    // which the backend rejects.
    private val LICENSE = Regex("""^[0-9]{3,6}$""")
    private val DNI = Regex("""^[0-9]{8}$""")
    private val HOUR = Regex("""^([01]\d|2[0-3]):[0-5]\d$""")

    const val DNI_LENGTH = 8
    const val LICENSE_MAX_LENGTH = 6
    const val FULL_NAME_MAX_LENGTH = 120
    private const val PASSWORD_MAX_BYTES = 72

    fun isValidEmail(value: String): Boolean = EMAIL.matches(value.trim())

    /** At least 8 characters with letters and numbers (same as the backend's PasswordPolicy). */
    fun isStrongPassword(value: String): Boolean = passwordError(value) == null

    fun passwordError(value: String): String? = when {
        value.length < 8 -> "La contraseña debe tener al menos 8 caracteres"
        value.none { it.isLetter() } || value.none { it.isDigit() } -> "La contraseña debe incluir al menos una letra y un número"
        value.toByteArray(Charsets.UTF_8).size > PASSWORD_MAX_BYTES -> "La contraseña es demasiado larga"
        else -> null
    }

    /** CPsP license number: 3 to 6 ASCII digits. */
    fun isValidLicense(value: String): Boolean = LICENSE.matches(value.trim())

    /** Peruvian DNI: exactly 8 ASCII digits (format only; it is not checked against RENIEC). */
    fun isValidDni(value: String): Boolean = DNI.matches(value.trim())

    /** Backend message for the DNI, or null when it is valid. */
    fun dniError(value: String): String? = when {
        value.isBlank() -> "El DNI es obligatorio"
        !isValidDni(value) -> "El DNI debe tener exactamente 8 dígitos numéricos"
        else -> null
    }

    /** Keeps only ASCII digits 0-9 (also when pasting) and limits the length. */
    fun digitsOnly(value: String, max: Int = Int.MAX_VALUE): String = value.filter { it in '0'..'9' }.take(max)

    /** "12345678" → "****5678". */
    fun maskDni(value: String): String {
        val dni = value.trim()
        return if (dni.length > 4) "*".repeat(dni.length - 4) + dni.takeLast(4) else dni
    }

    fun isValidHour(value: String): Boolean = HOUR.matches(value.trim())

    fun isValidStressLevel(value: Int): Boolean = value in 1..10

    /** Trims and collapses repeated spaces: "  Ana   Torres " → "Ana Torres" (same as the backend). */
    fun normalizeFullName(value: String): String = value.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")

    /**
     * Backend message for the full name, or null when it is valid: first name and last name, up to 120 characters,
     * letters of any alphabet (accents, ñ, ü...), spaces, apostrophes, hyphens and periods.
     */
    fun fullNameError(value: String): String? {
        val name = normalizeFullName(value)
        return when {
            name.isEmpty() -> "Ingresa tu nombre y apellido"
            name.length > FULL_NAME_MAX_LENGTH -> "El nombre no puede superar 120 caracteres"
            !name.all(::isNameCharacter) -> "El nombre solo puede contener letras, espacios, apóstrofos, guiones y puntos"
            name.split(' ').size < 2 || name.none { it.isLetter() } -> "Ingresa tu nombre y apellido"
            else -> null
        }
    }

    fun isValidFullName(value: String): Boolean = fullNameError(value) == null

    private fun isNameCharacter(c: Char): Boolean =
        c.isLetter() || Character.getType(c) == Character.NON_SPACING_MARK.toInt() ||
            Character.getType(c) == Character.COMBINING_SPACING_MARK.toInt() || c in " '’-."
}
