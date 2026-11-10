package com.example.neurozen_front.domain

import com.example.neurozen_front.domain.logic.BotSuggestion
import com.example.neurozen_front.domain.logic.CrisisDetector
import com.example.neurozen_front.domain.logic.ZenBotEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZenBotEngineTest {

    @Test
    fun crisis_isDetectedWithAccentsAndCase() {
        assertTrue(CrisisDetector.isCrisis("Ya no quiero vivir"))
        assertTrue(CrisisDetector.isCrisis("pienso en el SUICIDIO"))
        assertTrue(CrisisDetector.isCrisis("quiero hacerme daño"))
        assertTrue(CrisisDetector.isCrisis("a veces pienso en quitarme la vida"))
    }

    @Test
    fun normalMessages_areNotCrisis() {
        assertFalse(CrisisDetector.isCrisis("tengo mucho trabajo hoy"))
        assertFalse(CrisisDetector.isCrisis("me muero de hambre jaja"))
    }

    @Test
    fun crisisReply_hasPriorityAndHelplines() {
        val reply = ZenBotEngine.reply("estoy estresado por el trabajo y no quiero vivir")
        assertTrue(reply.isCrisis)
        assertTrue(reply.text.contains("113"))
        assertEquals(BotSuggestion.PROFESSIONALS, reply.suggestion)
    }

    @Test
    fun keywordRules() {
        assertEquals(BotSuggestion.BREATHING, ZenBotEngine.reply("tengo ansiedad").suggestion)
        assertEquals(BotSuggestion.TRIGGERS, ZenBotEngine.reply("mi jefe me presiona").suggestion)
        assertEquals(BotSuggestion.PROFESSIONALS, ZenBotEngine.reply("quiero ir a terapia").suggestion)
        assertFalse(ZenBotEngine.reply("hola").isCrisis)
    }

    @Test
    fun unknownMessage_getsDefaultReply() {
        val reply = ZenBotEngine.reply("xyz")
        assertFalse(reply.isCrisis)
        assertTrue(reply.text.isNotBlank())
    }
}
