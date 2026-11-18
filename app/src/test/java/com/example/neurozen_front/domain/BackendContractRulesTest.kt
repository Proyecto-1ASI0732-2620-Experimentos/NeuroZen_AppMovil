package com.example.neurozen_front.domain

import com.example.neurozen_front.data.remote.ErrorMapper
import com.example.neurozen_front.data.remote.ResourceDto
import com.example.neurozen_front.data.remote.toDomain
import com.example.neurozen_front.domain.logic.Catalogs
import com.example.neurozen_front.domain.logic.Validators
import com.example.neurozen_front.domain.logic.YouTube
import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.isPlanRequired
import com.example.neurozen_front.domain.model.isProfessionalNotVerified
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rules aligned with the audited backend: DNI, full name, CPsP license, catalogs, YouTube links and 403/429 errors. */
class BackendContractRulesTest {

    @Test
    fun dni_exactly8AsciiDigits() {
        assertEquals("El DNI es obligatorio", Validators.dniError(""))
        assertEquals("El DNI debe tener exactamente 8 dígitos numéricos", Validators.dniError("1234567"))
        assertEquals("El DNI debe tener exactamente 8 dígitos numéricos", Validators.dniError("123456789"))
        assertEquals("El DNI debe tener exactamente 8 dígitos numéricos", Validators.dniError("1234567A"))
        assertEquals("El DNI debe tener exactamente 8 dígitos numéricos", Validators.dniError("1234-567"))
        assertEquals("El DNI debe tener exactamente 8 dígitos numéricos", Validators.dniError("1234 5678"))
        // Arabic-Indic digits: Char.isDigit() accepts them, the backend does not
        assertEquals("El DNI debe tener exactamente 8 dígitos numéricos", Validators.dniError("١٢٣٤٥٦٧٨"))
        assertNull(Validators.dniError(" 12345678 "))
    }

    @Test
    fun digitsOnly_filtersPastedTextAndLimits() {
        assertEquals("12345678", Validators.digitsOnly("12a3-4 5.6789", 8))
        assertEquals("45821", Validators.digitsOnly("C.Ps.P. 45821", 6))
        assertEquals("", Validators.digitsOnly("١٢٣", 8))
        assertEquals("****5678", Validators.maskDni("12345678"))
    }

    @Test
    fun fullName_backendRules() {
        assertNull(Validators.fullNameError("María-José O'Connor"))
        assertNull(Validators.fullNameError("Íñigo Peña Müller"))
        assertNull(Validators.fullNameError("José D’Alessandro"))
        assertNull(Validators.fullNameError("Алексей Иванов"))
        assertEquals("Ingresa tu nombre y apellido", Validators.fullNameError("Lucía"))
        assertEquals("El nombre solo puede contener letras, espacios, apóstrofos, guiones y puntos", Validators.fullNameError("Lucía 2 Mendoza"))
        assertEquals("El nombre solo puede contener letras, espacios, apóstrofos, guiones y puntos", Validators.fullNameError("Lucía M@ndoza"))
        assertEquals("El nombre no puede superar 120 caracteres", Validators.fullNameError("Ana " + "a".repeat(120)))
        assertEquals("Ana Torres", Validators.normalizeFullName("  Ana   Torres "))
    }

    @Test
    fun license_3to6AsciiDigits() {
        assertTrue(Validators.isValidLicense("123"))
        assertTrue(Validators.isValidLicense("123456"))
        assertFalse(Validators.isValidLicense("12"))
        assertFalse(Validators.isValidLicense("1234567")) // antes se aceptaban hasta 8
        assertFalse(Validators.isValidLicense("٤٥٨٢١"))
    }

    @Test
    fun password_72BytesLimitLikeBackend() {
        assertNull(Validators.passwordError("Clave2026"))
        assertEquals("La contraseña es demasiado larga", Validators.passwordError("Ñ".repeat(40) + "1"))
    }

    @Test
    fun catalogs_matchBackend() {
        assertEquals(listOf("clinical", "organizational", "educational", "family", "childAdolescent", "health", "social", "neuropsychology"),
            Catalogs.SPECIALTIES.map { it.key })
        assertEquals("Psicología clínica", Catalogs.specialtyLabel("clinical"))
        assertEquals("Texto antiguo", Catalogs.specialtyLabel("Texto antiguo"))
        assertEquals(listOf("formal", "informal"), Catalogs.WORK_TYPES.map { it.key })
        assertEquals(listOf("Básico", "Avanzado", "Profesional"), Catalogs.PLANS.map { it.name })
    }

    @Test
    fun youTube_idsAndUrls() {
        assertEquals("CVMBSsqTGOk", YouTube.videoId("https://www.youtube.com/watch?v=CVMBSsqTGOk"))
        assertEquals("bkQwODQAdCo", YouTube.videoId("https://m.youtube.com/watch?v=bkQwODQAdCo"))
        assertEquals("H0pbvdIcHOY", YouTube.videoId("https://youtu.be/H0pbvdIcHOY"))
        assertEquals("tS52C0Pxf_g", YouTube.videoId("https://www.youtube.com/watch?feature=share&v=tS52C0Pxf_g"))
        assertNull(YouTube.videoId("https://www.youtube.com/watch?v=example1"))
        assertNull(YouTube.videoId("https://neurozen.com/audio/musica.mp3"))
        assertEquals("https://www.youtube-nocookie.com/embed/pyAyEzmiKxU?rel=0&playsinline=1", YouTube.embedUrl("pyAyEzmiKxU"))
    }

    @Test
    fun resourceMapping_youTubeAudioGetsThumbnailAndKeepsClinicalNotice() {
        val notice = "Este material no sustituye la atención de un profesional de la salud."
        val audio = ResourceDto(9, "Podcast: Manejo de Crisis de Ansiedad", "Descripción. $notice", null, "Audio",
            "https://www.youtube.com/watch?v=A8qOLnTbQdg", 25, "", emptyList(), "", null).toDomain()
        assertEquals("https://img.youtube.com/vi/A8qOLnTbQdg/hqdefault.jpg", audio.thumbnail)
        assertTrue(audio.description.endsWith(notice))
        assertNull(audio.author)
    }

    @Test
    fun errors_403PlanAndNotVerified_and429() {
        val plan = ErrorMapper.fromHttp(403, """{"message":"Este recurso está disponible desde el plan Avanzado"}""")
        val notVerified = ErrorMapper.fromHttp(403, """{"message":"Tu cuenta profesional aún no está verificada"}""")
        assertTrue(plan.isPlanRequired())
        assertFalse(plan.isProfessionalNotVerified())
        assertTrue(notVerified.isProfessionalNotVerified())
        assertFalse(notVerified.isPlanRequired())
        assertTrue(ErrorMapper.fromHttp(429, null) is AppError.TooManyRequests)
    }
}
