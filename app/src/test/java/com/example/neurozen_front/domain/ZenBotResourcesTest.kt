package com.example.neurozen_front.domain

import com.example.neurozen_front.domain.logic.BotSuggestion
import com.example.neurozen_front.domain.logic.ZenBotEngine
import com.example.neurozen_front.domain.logic.ZenBotResources
import com.example.neurozen_front.domain.model.LibraryResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Z1: resource recommendations never apply to crisis replies. */
class ZenBotResourcesTest {

    @Test
    fun crisis_neverSearchesResources() {
        val reply = ZenBotEngine.reply("no puedo dormir y quiero morir")
        assertTrue(reply.isCrisis)
        assertNull(ZenBotResources.queryFor("no puedo dormir y quiero morir", reply))
    }

    @Test
    fun queryFor_topics() {
        assertEquals("sueño", ZenBotResources.queryFor("No puedo dormir bien", ZenBotEngine.reply("No puedo dormir bien")))
        assertEquals("respiración", ZenBotResources.queryFor("tengo ansiedad", ZenBotEngine.reply("tengo ansiedad")))
        assertEquals("pausa activa", ZenBotResources.queryFor("me duele el cuello", ZenBotEngine.reply("me duele el cuello")))
        assertEquals("estrés", ZenBotResources.queryFor("Recomiéndame un video", ZenBotEngine.reply("Recomiéndame un video")))
        assertNull(ZenBotResources.queryFor("quiero ir a terapia", ZenBotEngine.reply("quiero ir a terapia")))
    }

    @Test
    fun newRules_leadToRealScreens() {
        assertEquals(BotSuggestion.MY_APPOINTMENTS, ZenBotEngine.reply("Ver mis citas").suggestion)
        assertEquals(BotSuggestion.PROFESSIONALS, ZenBotEngine.reply("Reservar una cita").suggestion)
        assertEquals(BotSuggestion.TRIGGERS, ZenBotEngine.reply("Registrar cómo me siento").suggestion)
        assertEquals(BotSuggestion.BREATHING, ZenBotEngine.reply("Me siento estresado/a").suggestion)
        assertEquals(BotSuggestion.ASSESSMENT, ZenBotEngine.reply("Hacer el test de estrés").suggestion)
        assertEquals(BotSuggestion.BREAKS, ZenBotEngine.reply("Hacer una pausa activa").suggestion)
    }

    private fun resource(id: Long, title: String, tags: List<String> = emptyList(), description: String = "") =
        LibraryResource(id, title, description, null, "Video", null, 5, null, tags, null)

    @Test
    fun filterLocally_isAccentInsensitive_andLimited() {
        val all = listOf(
            resource(1, "Respiración para dormir"),
            resource(2, "Pausas activas", tags = listOf("respiracion")),
            resource(3, "Otro", description = "Ejercicio de RESPIRACIÓN"),
            resource(4, "Nada que ver"),
            resource(5, "Respiración 4-7-8")
        )
        assertEquals(listOf(1L, 2L, 3L), ZenBotResources.filterLocally(all, "respiración").map { it.id })
        assertEquals(listOf(1L, 2L), ZenBotResources.filterLocally(all, "respiracion", limit = 2).map { it.id })
        assertTrue(ZenBotResources.filterLocally(all, "  ").isEmpty())
    }
}
