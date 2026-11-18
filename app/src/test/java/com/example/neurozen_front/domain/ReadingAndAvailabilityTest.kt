package com.example.neurozen_front.domain

import com.example.neurozen_front.domain.logic.AvailabilityDraft
import com.example.neurozen_front.domain.logic.ReadingBlock
import com.example.neurozen_front.domain.logic.ReadingContent
import com.example.neurozen_front.domain.model.AvailabilitySchedule
import com.example.neurozen_front.domain.model.LibraryResource
import com.example.neurozen_front.presentation.worker.ResourceKind
import com.example.neurozen_front.presentation.worker.durationLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class ReadingAndAvailabilityTest {

    @Test
    fun reading_headingsParagraphsAndLists() {
        val blocks = ReadingContent.parse("Título corto\n\nUn párrafo.\nSigue aquí.\n\n- uno\n- dos\n\n1. paso\n2. otro\n\nTexto final.")
        assertEquals(
            listOf(
                ReadingBlock.Heading("Título corto"),
                ReadingBlock.Paragraph("Un párrafo.\nSigue aquí."),
                ReadingBlock.Bullets(false, listOf("uno", "dos")),
                ReadingBlock.Bullets(true, listOf("paso", "otro")),
                ReadingBlock.Paragraph("Texto final.")
            ),
            blocks
        )
    }

    @Test
    fun reading_htmlStaysAsText_crlfAndEmpty() {
        val html = ReadingContent.parse("Esto <b>no</b> es HTML <script>x</script>.")
        assertEquals(listOf(ReadingBlock.Paragraph("Esto <b>no</b> es HTML <script>x</script>.")), html)
        assertEquals(listOf(ReadingBlock.Heading("A"), ReadingBlock.Paragraph("B.")), ReadingContent.parse("A\r\n\r\nB."))
        assertTrue(ReadingContent.parse("  ").isEmpty())
        assertEquals(listOf(ReadingBlock.Paragraph("Respira.")), ReadingContent.parse("Respira."))
    }

    @Test
    fun availability_defaultsValidationAndEnglishDays() {
        val draft = AvailabilityDraft.from(null)
        assertEquals(AvailabilityDraft.DEFAULT_DAYS, draft.days)
        assertTrue(draft.isValid)
        assertEquals(listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"), draft.toSchedule().days)
        assertEquals("Selecciona al menos un día de atención", draft.copy(days = emptySet()).error)
        assertEquals("La hora de fin debe ser posterior a la de inicio", draft.copy(end = "08:00").error)
        assertEquals("Usa el formato HH:mm (por ejemplo 09:00)", draft.copy(start = "9").error)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY), draft.toggle(DayOfWeek.FRIDAY).days)
    }

    @Test
    fun availability_spanishSavedDaysAreSentInEnglish() {
        // The backend only accepts monday…sunday: an old schedule saved as "Lunes" is re-sent in English
        val draft = AvailabilityDraft.from(AvailabilitySchedule(listOf("Lunes", "Miércoles"), "10:00", "14:00"))
        assertEquals(listOf("MONDAY", "WEDNESDAY"), draft.toSchedule().days)
        assertTrue(AvailabilityDraft.hasSchedule(AvailabilitySchedule(listOf("monday"), "09:00", "10:00")))
        assertFalse(AvailabilityDraft.hasSchedule(null))
        assertFalse(AvailabilityDraft.hasSchedule(AvailabilitySchedule(emptyList(), "", "")))
    }

    private fun resource(type: String, minutes: Int) = LibraryResource(1, "t", "d", null, type, null, minutes, null, emptyList(), null)

    @Test
    fun resourceKindsAndDurationLabels() {
        assertEquals(ResourceKind.READING, ResourceKind.of("Article"))
        assertEquals(ResourceKind.EXERCISE, ResourceKind.of("Exercise"))
        assertEquals(ResourceKind.AUDIO, ResourceKind.of("Audio"))
        assertEquals(ResourceKind.VIDEO, ResourceKind.of("Video"))
        assertEquals("5 min de lectura", durationLabel(resource("Article", 5)))
        assertEquals("6 min", durationLabel(resource("Exercise", 6)))
        assertNull(null)
    }
}
