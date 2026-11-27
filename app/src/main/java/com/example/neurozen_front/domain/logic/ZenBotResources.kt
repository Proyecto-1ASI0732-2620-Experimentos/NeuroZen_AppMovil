package com.example.neurozen_front.domain.logic

import com.example.neurozen_front.domain.model.LibraryResource

/**
 * Which real library resources ZenBot recommends after a (non-crisis) reply. Pure, so it is unit tested.
 * Crisis replies never trigger a search: the help lines are shown immediately and alone.
 */
object ZenBotResources {
    const val MAX_RESULTS = 3

    private val SLEEP = listOf("dorm", "sueno", "insomn", "descans")

    /** Search text for the library, or null when this reply should not recommend resources. */
    fun queryFor(input: String, reply: BotReply): String? {
        if (reply.isCrisis) return null
        val text = CrisisDetector.normalize(input)
        return when {
            SLEEP.any { text.contains(it) } -> "sueño"
            reply.suggestion == BotSuggestion.BREATHING -> "respiración"
            reply.suggestion == BotSuggestion.BREAKS -> "pausa activa"
            reply.suggestion == BotSuggestion.LIBRARY -> "estrés"
            else -> null
        }
    }

    /** Local fallback when /search is not available: title, description or tag containing the query (accent-insensitive). */
    fun filterLocally(all: List<LibraryResource>, query: String, limit: Int = MAX_RESULTS): List<LibraryResource> {
        val q = CrisisDetector.normalize(query)
        if (q.isEmpty()) return emptyList()
        return all.filter { r ->
            CrisisDetector.normalize(r.title).contains(q) || CrisisDetector.normalize(r.description).contains(q) ||
                r.tags.any { CrisisDetector.normalize(it).contains(q) }
        }.take(limit)
    }
}
