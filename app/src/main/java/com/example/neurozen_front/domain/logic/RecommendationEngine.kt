package com.example.neurozen_front.domain.logic

import com.example.neurozen_front.domain.model.StressLevel

enum class RecommendationAction { BREATHING, BREAKS, LIBRARY, PROFESSIONALS, ASSESSMENT, WORKLOAD, TRIGGERS }

data class Recommendation(val title: String, val description: String, val action: RecommendationAction)

/** Personalised recommendations (US07) from the latest level, top trigger category and weekly hours. */
object RecommendationEngine {
    const val WEEKLY_HOURS_LIMIT = 48.0

    fun recommend(
        level: StressLevel?,
        topCategory: String?,
        weeklyHours: Double?,
        hasAssessment: Boolean
    ): List<Recommendation> {
        val result = mutableListOf<Recommendation>()
        if (!hasAssessment) {
            result += Recommendation("Haz tu primer test", "Conoce tu nivel de estrés en 3 minutos.", RecommendationAction.ASSESSMENT)
        }
        when (level) {
            StressLevel.HIGH -> {
                result += Recommendation("Habla con un profesional", "Tu nivel es alto. Un psicólogo verificado puede ayudarte.", RecommendationAction.PROFESSIONALS)
                result += Recommendation("Respiración 4-7-8", "Calma tu sistema nervioso en 2 minutos.", RecommendationAction.BREATHING)
            }
            StressLevel.MODERATE -> {
                result += Recommendation("Respiración cuadrada", "4 tiempos para inhalar, sostener, exhalar y sostener.", RecommendationAction.BREATHING)
                result += Recommendation("Programa pausas activas", "Una pausa de 3 minutos cada hora reduce la tensión.", RecommendationAction.BREAKS)
            }
            StressLevel.LOW -> {
                result += Recommendation("Mantén tus hábitos", "Explora la biblioteca para seguir cuidándote.", RecommendationAction.LIBRARY)
            }
            null -> Unit
        }
        if (topCategory != null && topCategory.equals("work", ignoreCase = true)) {
            result += Recommendation("Cuida tu carga laboral", "El trabajo es tu principal fuente de estrés. Revisa tus horas semanales.", RecommendationAction.WORKLOAD)
        }
        if (weeklyHours != null && weeklyHours > WEEKLY_HOURS_LIMIT) {
            result += Recommendation(
                "Superas las 48 h semanales",
                "Registraste ${"%.1f".format(weeklyHours)} h esta semana. Intenta desconectarte fuera de tu horario.",
                RecommendationAction.WORKLOAD
            )
        }
        if (result.none { it.action == RecommendationAction.TRIGGERS }) {
            result += Recommendation("Registra tu estrés", "Anotar qué lo provoca te ayuda a ver patrones.", RecommendationAction.TRIGGERS)
        }
        return result.distinctBy { it.title }
    }
}
