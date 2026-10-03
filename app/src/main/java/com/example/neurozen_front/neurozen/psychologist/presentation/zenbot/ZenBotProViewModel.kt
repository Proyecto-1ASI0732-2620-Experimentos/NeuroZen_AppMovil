package com.example.neurozen_front.neurozen.psychologist.presentation.zenbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessagePro(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ZenBotProState(
    val messages: List<MessagePro> = listOf(
        MessagePro("🩺 ¡Bienvenido a ZenBot Pro! Soy tu Asistente Clínico de NeuroZen. ¿En qué puedo asistirte hoy? Puedo ayudarte a redactar notas de sesión (formato SOAP), sugerir ejercicios de TCC/ACT, consultar guías psicoeducativas o brindarte pautas para prevenir el burnout del terapeuta.", false)
    ),
    val inputText: String = "",
    val isTyping: Boolean = false
)

@HiltViewModel
class ZenBotProViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(ZenBotProState())
    val state: StateFlow<ZenBotProState> = _state.asStateFlow()

    private val clinicalResponses = mapOf(
        "soap" to """
            📋 *Estructura de Nota SOAP para Sesión*:
            
            • *S (Subjetivo)*: Síntomas y vivencias reportadas por el paciente.
            • *O (Objetivo)*: Observaciones clínicas, afecto, lenguaje no verbal.
            • *A (Análisis)*: Evaluación diagnóstica, progreso e hipótesis evolutiva.
            • *P (Plan)*: Tareas entre sesiones, técnicas a aplicar y fecha de próximo encuentro.
        """.trimIndent(),

        "ansiedad" to """
            🧠 *Técnicas recomendadas para Trastornos de Ansiedad en consulta*:
            
            1. *Reestructuración Cognitiva*: Registro de pensamientos automáticos negativos (PAN).
            2. *Exposición Gradual*: Jerarquía de desensibilización sistemática.
            3. *Desactivación Fisiológica*: Respiración diafragmática y Relajación Muscular Progresiva de Jacobson.
            4. *Mindfulness Clínico*: Ejercicios de anclaje sensorial 5-4-3-2-1.
        """.trimIndent(),

        "depresion" to """
            🌱 *Pautas TCC para Sintomatología Depresiva*:
            
            • *Activación Conductual (BA)*: Programación de actividades placenteras y de dominio.
            • *Identificación de Distorsiones Cognitivas*: Sobregeneralización, inferencia arbitraria y pensamiento todo o nada.
            • *Auto-comprensión y validación emocional* en primeras fases del encuadre.
        """.trimIndent(),

        "burnout" to """
            💚 *Autocuidado para Psicólogos y Terapeutas*:
            
            • *Límites claros*: Define un horario estricto para revisión de mensajes fuera de consulta.
            • *Supervisión de caso*: Comparte casos complejos con tu grupo de intervisión.
            • *Micro-pausas respiratorias*: Tómate 5 minutos de pausa consciente entre cada paciente.
            • *Higiene del sueño*: Desconéctate de la pantalla al menos 45 min antes de dormir.
        """.trimIndent(),

        "act" to """
            🌀 *Intervenciones basadas en Terapia de Aceptación y Compromiso (ACT)*:
            
            • *Defusión Cognitiva*: Ejercicio de observar pensamientos como nubes en el cielo.
            • *Clarificación de Valores*: Matriz de valores personales frente a barreras emocionales.
            • *Yo como Contexto*: Distinción entre el observador consciente y los contenidos mentales.
        """.trimIndent(),

        "tarea" to """
            📝 *Sugerencia de Tareas entre Sesiones*:
            
            • Diarios de registro emocional (Situación - Pensamiento - Emoción - Conducta).
            • Micro-experimentos conductuales para poner a prueba creencias limitantes.
            • Practicar 10 minutos de meditación/respiración diaria con la app de NeuroZen.
        """.trimIndent()
    )

    fun onInputChange(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isEmpty()) return

        val userMessage = MessagePro(text, true)
        _state.update { it.copy(
            messages = it.messages + userMessage,
            inputText = "",
            isTyping = true
        ) }

        viewModelScope.launch {
            delay(1200)
            val responseText = findClinicalResponse(text)
            val botMessage = MessagePro(responseText, false)
            _state.update { it.copy(
                messages = it.messages + botMessage,
                isTyping = false
            ) }
        }
    }

    private fun findClinicalResponse(input: String): String {
        val lowerInput = input.lowercase()
        for ((key, response) in clinicalResponses) {
            if (lowerInput.contains(key)) return response
        }
        return "Comprendido, Colega. He registrado tu observación. Te sugiero revisar las guías clínicas TCC/ACT disponibles en la sección de Recursos o utilizar el formato de nota SOAP para formalizar el expediente del paciente. ✨"
    }
}
