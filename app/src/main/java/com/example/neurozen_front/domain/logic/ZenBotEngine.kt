package com.example.neurozen_front.domain.logic

data class BotReply(val text: String, val isCrisis: Boolean = false, val suggestion: BotSuggestion? = null)

enum class BotSuggestion { BREATHING, TRIGGERS, ASSESSMENT, PROFESSIONALS, BREAKS, LIBRARY }

/** Rule-based assistant for the worker app. Crisis detection always wins over any other rule. */
object ZenBotEngine {

    const val CRISIS_MESSAGE =
        "Siento mucho que estés pasando por esto. No estás solo/a. Si estás en peligro, llama ahora al 105 (Policía) " +
            "o al 106 (SAMU). Para hablar con un profesional de salud mental, llama gratis a la Línea 113, opción 5 (MINSA), " +
            "disponible las 24 horas. También puedes acudir a la emergencia más cercana o pedirle a alguien de confianza que te acompañe."

    const val GREETING =
        "¡Hola! Soy ZenBot 🌿. Puedo sugerirte ejercicios de respiración, pausas activas o ayudarte a registrar lo que te estresa. " +
            "No reemplazo a un profesional. ¿Cómo te sientes hoy?"

    private data class Rule(val keywords: List<String>, val reply: BotReply)

    private val rules = listOf(
        Rule(listOf("respir", "ansie", "nervios", "panico", "agitad"), BotReply(
            "Probemos respirar juntos: inhala en 4 tiempos, sostén 4 y exhala en 6. En Bienestar > Respiración tienes ejercicios guiados.",
            suggestion = BotSuggestion.BREATHING
        )),
        Rule(listOf("trabajo", "jefe", "reunion", "plazo", "carga", "horas extra"), BotReply(
            "El estrés laboral es muy común. Registrar qué lo provoca ayuda a ver patrones. ¿Quieres anotarlo ahora?",
            suggestion = BotSuggestion.TRIGGERS
        )),
        Rule(listOf("dormir", "insomnio", "sueno", "cansad"), BotReply(
            "Descansar bien es clave. Intenta dejar las pantallas 30 minutos antes de dormir y mantener un horario fijo. " +
                "Si llevas semanas sin dormir bien, conversa con un profesional.",
            suggestion = BotSuggestion.LIBRARY
        )),
        Rule(listOf("test", "evalua", "nivel"), BotReply(
            "El test de autoevaluación tiene 10 preguntas y toma unos 3 minutos. Te dará tu nivel de estrés actual.",
            suggestion = BotSuggestion.ASSESSMENT
        )),
        Rule(listOf("psicolog", "terapia", "profesional", "cita", "ayuda profesional"), BotReply(
            "Puedes reservar una sesión con un psicólogo verificado por el CPsP desde la pestaña Profesionales.",
            suggestion = BotSuggestion.PROFESSIONALS
        )),
        Rule(listOf("pausa", "estir", "espalda", "cuello", "postura"), BotReply(
            "Una pausa activa de 2-3 minutos cada hora reduce la tensión muscular. Puedes programar recordatorios en Bienestar > Pausas activas.",
            suggestion = BotSuggestion.BREAKS
        )),
        Rule(listOf("gracias"), BotReply("¡Con gusto! Cuídate mucho. Aquí estaré si me necesitas.")),
        Rule(listOf("hola", "buenas", "buenos dias", "buenas tardes"), BotReply("¡Hola! ¿Qué te gustaría trabajar hoy: respiración, pausas o registrar tu estrés?"))
    )

    fun reply(input: String): BotReply {
        if (CrisisDetector.isCrisis(input)) return BotReply(CRISIS_MESSAGE, isCrisis = true, suggestion = BotSuggestion.PROFESSIONALS)
        val normalized = CrisisDetector.normalize(input)
        return rules.firstOrNull { rule -> rule.keywords.any { normalized.contains(it) } }?.reply
            ?: BotReply("Gracias por contármelo. Expresar lo que sientes ya es un paso. ¿Quieres probar un ejercicio de respiración o registrar este momento?")
    }
}
