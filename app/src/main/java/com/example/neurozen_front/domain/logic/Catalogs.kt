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

    val PLANS = listOf(
        SubscriptionPlan(1, "Básico", 0.0, listOf("Registro de estrés", "Test de autoevaluación", "Respiración guiada")),
        SubscriptionPlan(2, "Premium", 29.90, listOf("Todo lo del plan Básico", "Biblioteca completa", "Informes para tu psicólogo")),
        SubscriptionPlan(3, "Empresarial", 99.90, listOf("Todo lo de Premium", "Acompañamiento para equipos", "Soporte prioritario"))
    )

    data class BreathingExercise(val id: String, val name: String, val description: String, val phases: List<Pair<String, Int>>, val cycles: Int)

    val BREATHING_EXERCISES = listOf(
        BreathingExercise("box", "Respiración cuadrada", "Equilibra y enfoca. Ideal antes de una reunión.",
            listOf("Inhala" to 4, "Sostén" to 4, "Exhala" to 4, "Sostén" to 4), 4),
        BreathingExercise("478", "Respiración 4-7-8", "Relaja el sistema nervioso. Útil para dormir.",
            listOf("Inhala" to 4, "Sostén" to 7, "Exhala" to 8), 4),
        BreathingExercise("coherent", "Respiración coherente", "5 respiraciones por minuto para bajar la tensión.",
            listOf("Inhala" to 6, "Exhala" to 6), 5)
    )

    data class Challenge(val id: String, val title: String, val description: String, val days: Int)

    val CHALLENGES = listOf(
        Challenge("breath7", "7 días respirando", "Completa un ejercicio de respiración cada día.", 7),
        Challenge("disconnect5", "Desconexión digital", "Sin correos de trabajo después de las 8 p. m. durante 5 días.", 5),
        Challenge("walk7", "Caminata diaria", "Camina 15 minutos al día durante una semana.", 7),
        Challenge("gratitude10", "Gratitud", "Anota 3 cosas buenas de tu día durante 10 días.", 10)
    )
}
