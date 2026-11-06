package com.example.neurozen_front.domain.logic

import java.text.Normalizer

/**
 * Keyword based detection (same strategy as the web ZenBot). It runs locally, so it also works offline.
 * It does not diagnose: it only decides when to show crisis help resources first.
 */
object CrisisDetector {
    private val CRISIS_PATTERNS = listOf(
        "suicid", "matarme", "quitarme la vida", "no quiero vivir", "quiero morir", "me quiero morir",
        "acabar con todo", "acabar con mi vida", "hacerme dano", "autolesion", "cortarme", "no vale la pena vivir",
        "desaparecer para siempre", "mejor muerto", "mejor muerta"
    )

    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun isCrisis(text: String): Boolean {
        val normalized = normalize(text)
        return CRISIS_PATTERNS.any { normalized.contains(it) }
    }
}
