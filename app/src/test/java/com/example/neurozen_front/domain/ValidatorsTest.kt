package com.example.neurozen_front.domain

import com.example.neurozen_front.domain.logic.DayCodec
import com.example.neurozen_front.domain.logic.JwtDecoder
import com.example.neurozen_front.domain.logic.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.util.Base64

class ValidatorsTest {

    @Test
    fun email() {
        assertTrue(Validators.isValidEmail("ana.perez@empresa.pe"))
        assertFalse(Validators.isValidEmail("ana@"))
        assertFalse(Validators.isValidEmail("sin-arroba.com"))
    }

    @Test
    fun password() {
        assertTrue(Validators.isStrongPassword("Clave2026"))
        assertEquals("Mínimo 8 caracteres", Validators.passwordError("a1"))
        assertEquals("Debe tener letras y números", Validators.passwordError("solamenteletras"))
    }

    @Test
    fun licenseAndHours() {
        assertTrue(Validators.isValidLicense("12345"))
        assertFalse(Validators.isValidLicense("CPsP-123"))
        assertTrue(Validators.isValidHour("09:30"))
        assertFalse(Validators.isValidHour("24:00"))
        assertFalse(Validators.isValidFullName("Ana"))
        assertTrue(Validators.isValidFullName("Ana Pérez"))
    }

    @Test
    fun jwtExpiration_isRead() {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"1","exp":1893456000}""".toByteArray())
        assertEquals(1893456000L, JwtDecoder.expirationEpochSeconds("xxx.$payload.yyy"))
        assertNull(JwtDecoder.expirationEpochSeconds("no-es-jwt"))
    }

    @Test
    fun dayCodec_keepsBackendStyle() {
        assertEquals(DayCodec.Style.SPANISH, DayCodec.detectStyle(listOf("Lunes")))
        assertEquals(DayCodec.Style.ENGLISH_SHORT, DayCodec.detectStyle(listOf("MON")))
        assertEquals(DayCodec.Style.ENGLISH_FULL, DayCodec.detectStyle(emptyList()))
        assertEquals(DayOfWeek.WEDNESDAY, DayCodec.parse("miércoles"))
        assertEquals(DayOfWeek.FRIDAY, DayCodec.parse("Fri"))
        assertEquals("Sábado", DayCodec.encode(DayOfWeek.SATURDAY, DayCodec.Style.SPANISH))
        assertEquals("Lunes a Viernes, 09:00 - 18:00",
            DayCodec.summary(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), "09:00", "18:00"))
    }
}
