package com.example.neurozen_front.domain.logic

import com.example.neurozen_front.domain.model.SubscriptionPlan

/** Static catalogs shared by screens and tests. */
object Catalogs {

    data class Category(val key: String, val label: String)

    /** Keys sent to the backend; unknown keys are stored there as "other". */
    val TRIGGER_CATEGORIES = listOf(
        Category("work", "Trabajo"),
        Category("personal", "Personal"),
        Category("health", "Salud"),
        Category("family", "Familia"),
        Category("financial", "Finanzas"),
        Category("social", "Social"),
        Category("other", "Otro")
    )

    fun categoryLabel(key: String): String =
        TRIGGER_CATEGORIES.firstOrNull { it.key.equals(key, ignoreCase = true) }?.label ?: key.replaceFirstChar { it.uppercase() }

    val SYMPTOMS = listOf("Dolor de cabeza", "Insomnio", "Tensión muscular", "Fatiga", "Problemas digestivos", "Palpitaciones", "Irritabilidad", "Dificultad para concentrarse")

    val ASSESSMENT_QUESTIONS = listOf(
        "En el último mes, ¿con qué frecuencia te has sentido afectado/a por algo que ocurrió inesperadamente?",
        "En el último mes, ¿con qué frecuencia has sentido que no podías controlar las cosas importantes de tu vida?",
        "En el último mes, ¿con qué frecuencia te has sentido nervioso/a o estresado/a?",
        "En el último mes, ¿con qué frecuencia has confiado en tu capacidad para manejar tus problemas personales?",
        "En el último mes, ¿con qué frecuencia has sentido que las cosas te iban bien?",
        "En el último mes, ¿con qué frecuencia has sentido que no podías con todo lo que tenías que hacer?",
        "En el último mes, ¿con qué frecuencia has podido controlar las situaciones que te irritan?",
        "En el último mes, ¿con qué frecuencia has sentido que tenías todo bajo control?",
        "En el último mes, ¿con qué frecuencia te has enfadado por cosas que estaban fuera de tu control?",
        "En el último mes, ¿con qué frecuencia has sentido que las dificultades se acumulaban tanto que no podías superarlas?"
    )

    val ANSWER_OPTIONS = listOf("Nunca", "Casi nunca", "A veces", "Con frecuencia", "Muy a menudo")

    /**
     * planId 1 = Básico, 2 = Avanzado, 3 = Profesional: the same ids and names the backend uses (its library answers
     * "Este recurso está disponible desde el plan Avanzado") and the web shows.
     */
    val PLANS = listOf(
        SubscriptionPlan(1, "Básico", listOf("Registro de estrés", "Test de autoevaluación", "Parte de la biblioteca (videos y audios)")),
        SubscriptionPlan(2, "Avanzado", listOf("Todo lo del plan Básico", "Biblioteca completa", "Informes para tu psicólogo")),
        SubscriptionPlan(3, "Profesional", listOf("Todo lo del plan Avanzado", "Contenido exclusivo de la biblioteca", "Soporte prioritario"))
    )

    fun planName(planId: Int?): String = PLANS.firstOrNull { it.id == planId }?.name ?: "Básico"

    /** Closed catalog of the backend (values sent as-is); labels match the web. */
    val SPECIALTIES = listOf(
        Category("clinical", "Psicología clínica"),
        Category("organizational", "Psicología organizacional"),
        Category("educational", "Psicología educativa"),
        Category("family", "Psicología familiar y de pareja"),
        Category("childAdolescent", "Psicología infantil y del adolescente"),
        Category("health", "Psicología de la salud"),
        Category("social", "Psicología social y comunitaria"),
        Category("neuropsychology", "Neuropsicología")
    )

    /** "clinical" → "Psicología clínica"; unknown values (old free text) are shown as they come. */
    fun specialtyLabel(key: String?): String =
        SPECIALTIES.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) }?.label ?: key.orEmpty()

    /** Backend accepts only "formal" or "informal" (UserProfileRules.WorkTypes). */
    val WORK_TYPES = listOf(Category("formal", "Formal"), Category("informal", "Informal"))

    fun workTypeLabel(key: String?): String? =
        key?.let { k -> WORK_TYPES.firstOrNull { it.key.equals(k, ignoreCase = true) }?.label ?: k }

    /** Backend values (UserProfileRules.AgeRanges). */
    val AGE_RANGES = listOf("18-24", "25-34", "35-44", "45-54", "55+")

    data class BreathingExercise(val id: String, val name: String, val description: String, val phases: List<BreathPhase>, val cycles: Int)

    private fun inhale(seconds: Int) = BreathPhase(BreathPhaseKind.INHALE, seconds)
    private fun hold(seconds: Int) = BreathPhase(BreathPhaseKind.HOLD, seconds)
    private fun exhale(seconds: Int) = BreathPhase(BreathPhaseKind.EXHALE, seconds)

    val BREATHING_EXERCISES = listOf(
        BreathingExercise("box", "Respiración cuadrada", "Equilibra y enfoca. Ideal antes de una reunión.",
            listOf(inhale(4), hold(4), exhale(4), hold(4)), 4),
        BreathingExercise("478", "Respiración 4-7-8", "Relaja el sistema nervioso. Útil para dormir.",
            listOf(inhale(4), hold(7), exhale(8)), 4),
        BreathingExercise("coherent", "Respiración coherente", "5 respiraciones por minuto para bajar la tensión.",
            listOf(inhale(6), exhale(6)), 5)
    )

    data class Challenge(val id: String, val title: String, val description: String, val days: Int)

    val CHALLENGES = listOf(
        Challenge("breath7", "7 días respirando", "Completa un ejercicio de respiración cada día.", 7),
        Challenge("disconnect5", "Desconexión digital", "Sin correos de trabajo después de las 8 p. m. durante 5 días.", 5),
        Challenge("walk7", "Caminata diaria", "Camina 15 minutos al día durante una semana.", 7),
        Challenge("gratitude10", "Gratitud", "Anota 3 cosas buenas de tu día durante 10 días.", 10)
    )
}
