package com.example.neurozen_front.neurozen.home.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neurozen_front.neurozen.data.local.AppointmentDao
import com.example.neurozen_front.neurozen.data.local.AppointmentEntity
import com.example.neurozen_front.neurozen.data.network.DashboardResponse
import com.example.neurozen_front.neurozen.data.network.HealthMetricRequest
import com.example.neurozen_front.neurozen.data.network.MeditationResource
import com.example.neurozen_front.neurozen.data.network.NeurozenRepository
import com.example.neurozen_front.neurozen.data.session.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: NeurozenRepository,
    private val appointmentDao: AppointmentDao
) : ViewModel() {
    private val _homeState = MutableStateFlow(HomeState())
    val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

    private val headerImages = listOf(
        "https://images.unsplash.com/photo-1506126613408-eca07ce68773?q=80&w=1000",
        "https://images.unsplash.com/photo-1518199266791-5375a83190b7?q=80&w=1000",
        "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?q=80&w=1000",
        "https://images.unsplash.com/photo-1528715471579-d1bcf0ba5e83?q=80&w=1000"
    )

    init {
        viewModelScope.launch {
            UserSession.state.collect { session ->
                if (session.name != null) {
                    _homeState.update { current ->
                        current.copy(
                            user = current.user.copy(
                                name = session.name,
                                email = session.email ?: current.user.email
                            )
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            appointmentDao.getAllAppointments().collect { appointments ->
                _homeState.update { it.copy(upcomingAppointments = appointments) }
            }
        }
        refresh()
        loadMeditations()
        loadHealthHistory()
    }

    private fun loadHealthHistory() {
        viewModelScope.launch {
            if (!UserSession.hasActiveSession()) return@launch
            val userId = UserSession.state.value.userId ?: return@launch
            val token = UserSession.bearerTokenOrEmpty()
            repository.getHealthHistory(userId, token).onSuccess { history ->
                _homeState.update { it.copy(healthHistory = history) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _homeState.update { it.copy(
                isLoading = true, 
                error = null,
                headerImageUrl = headerImages.randomOrNull() ?: it.headerImageUrl
            ) }
            
            if (!UserSession.hasActiveSession()) {
                delay(300)
                _homeState.update { it.copy(isLoading = false, connectedToApi = false) }
                return@launch
            }

            val session = UserSession.state.value
            val userId = session.userId ?: return@launch

            try {
                val result = withContext(Dispatchers.IO) {
                    repository.fetchDashboard(
                        userId = userId,
                        bearerToken = UserSession.bearerTokenOrEmpty()
                    )
                }

                result.onSuccess { dashboard ->
                    _homeState.update { current ->
                        val updatedUser = current.user.copy(
                            name = session.name ?: current.user.name,
                            email = session.email ?: current.user.email
                        )
                        
                        dashboard.nextAppointment?.let { remote ->
                            viewModelScope.launch {
                                appointmentDao.insertAppointment(
                                    AppointmentEntity(
                                        psychologistId = remote.id.toString(),
                                        psychologistName = remote.professionalName,
                                        psychologistSpecialty = remote.professionalSpecialty,
                                        dateMillis = parseIsoDate(remote.appointmentDateTime),
                                        status = remote.status
                                    )
                                )
                            }
                        }

                        current.copy(
                            user = updatedUser,
                            stressLevel = dashboard.stressLevel,
                            sleepHours = dashboard.sleepHours.toFloat(),
                            healthMetrics = listOf(
                                HealthMetric("Estrés", "${dashboard.stressLevel}/10", if(dashboard.stressLevel > 7) "Nivel alto" else "Bajo control", dashboard.stressLevel / 10f),
                                HealthMetric("Sueño", "${dashboard.sleepHours} h", "Horas descansadas", dashboard.sleepHours / 12f),
                                HealthMetric("Pulso", "${dashboard.heartRate} bpm", "Ritmo cardiaco", dashboard.heartRate / 150f)
                            ),
                            connectedToApi = true,
                            isLoading = false,
                            error = null,
                            lastSync = "Sincronizado ${formatNow()}"
                        )
                    }
                }.onFailure { error ->
                    // MODO SILENCIOSO: Si falla el backend, no mostramos error al usuario
                    // simplemente mantenemos los datos locales cargados en init
                    _homeState.update { it.copy(
                        isLoading = false, 
                        connectedToApi = false,
                        error = null,
                        lastSync = "Modo local activo"
                    ) }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Dashboard exception", e)
                _homeState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun loadMeditations() {
        viewModelScope.launch {
            // Siempre aseguramos que haya sesiones, si el backend falla o está vacío usamos locales
            val token = UserSession.bearerTokenOrEmpty()
            repository.getMeditations(token).onSuccess { meditations ->
                val mapped = mapSessions(meditations)
                _homeState.update { it.copy(sessions = mapped.ifEmpty { getDefaultSessions() }) }
            }.onFailure {
                _homeState.update { it.copy(sessions = getDefaultSessions()) }
            }
        }
    }

    private fun getDefaultSessions(): List<MeditationSession> {
        return listOf(
            MeditationSession(
                id = "s1",
                title = "Respiración Profunda",
                durationMinutes = 10,
                type = "Respiración",
                difficulty = "Principiante",
                description = "Una guía simple para calmar el sistema nervioso.",
                benefit = "Reduce el estrés inmediato",
                status = "Disponible",
                imageUrl = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?q=80&w=1000"
            ),
            MeditationSession(
                id = "s2",
                title = "Calma Mental",
                durationMinutes = 15,
                type = "Meditación",
                difficulty = "Intermedio",
                description = "Encuentra paz en medio del caos diario.",
                benefit = "Mejora el enfoque",
                status = "Disponible",
                imageUrl = "https://images.unsplash.com/photo-1518199266791-5375a83190b7?q=80&w=1000"
            ),
            MeditationSession(
                id = "s3",
                title = "Sueño Reparador",
                durationMinutes = 20,
                type = "Relajación",
                difficulty = "Principiante",
                description = "Música y guía para conciliar el sueño.",
                benefit = "Dormir mejor",
                status = "Disponible",
                imageUrl = "https://images.unsplash.com/photo-1528715471579-d1bcf0ba5e83?q=80&w=1000"
            )
        )
    }

    private fun mapSessions(sessions: List<MeditationResource>): List<MeditationSession> {
        val backupImages = listOf(
            "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?q=80&w=1000",
            "https://images.unsplash.com/photo-1506126613408-eca07ce68773?q=80&w=1000",
            "https://images.unsplash.com/photo-1511295742364-917e7037a8ce?q=80&w=1000",
            "https://images.unsplash.com/photo-1528715471579-d1bcf0ba5e83?q=80&w=1000",
            "https://images.unsplash.com/photo-1508672019048-805c876b67e2?q=80&w=1000"
        )
        return sessions.mapIndexed { index, it -> 
            MeditationSession(
                id = it.id.toString(),
                title = it.title,
                durationMinutes = it.durationMinutes,
                type = if (it.title.lowercase().contains("respiración")) "Respiración" else "Meditación",
                difficulty = "Principiante",
                description = it.description,
                benefit = "Bienestar mental",
                status = "Disponible",
                imageUrl = if (it.imageUrl.isNullOrBlank()) backupImages[index % backupImages.size] else it.imageUrl,
                audioUrl = it.audioUrl
            )
        }.ifEmpty { emptyList() }
    }

    fun toggleTheme() {
        _homeState.update { current ->
            val nextTheme = if (current.user.themePreference == "Bosque claro") "Noche zen" else "Bosque claro"
            current.copy(user = current.user.copy(themePreference = nextTheme))
        }
    }

    fun setMood(mood: String) {
        _homeState.update { it.copy(selectedMood = mood) }
    }

    fun setShowEmotionalForm(show: Boolean) {
        _homeState.update { it.copy(showEmotionalForm = show) }
    }

    fun toggleFavorite(id: String) {
        _homeState.update { current ->
            current.copy(
                sessions = current.sessions.map {
                    if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
                }
            )
        }
    }

    fun updateSubscription(plan: String) {
        viewModelScope.launch {
            if (!UserSession.hasActiveSession()) return@launch
            val userId = UserSession.state.value.userId ?: return@launch
            val token = UserSession.bearerTokenOrEmpty()
            
            // Mapear nombre de plan a ID del backend
            val planId = when(plan) {
                "Básico" -> 1
                "Avanzado" -> 2
                "Zen+" -> 3
                else -> 1
            }

            val request = com.example.neurozen_front.neurozen.data.network.SubscriptionRequest(
                userId = userId,
                planId = planId,
                nameUser = _homeState.value.user.name,
                lastNameUser = "",
                emailUser = _homeState.value.user.email
            )

            val result = repository.createSubscription(request, token)
            result.onSuccess {
                _homeState.update { current ->
                    current.copy(user = current.user.copy(subscriptionPlan = plan))
                }
            }.onFailure { e ->
                _homeState.update { it.copy(error = e.message) }
            }
        }
    }

    fun submitEmotionalCheck(stress: Int, sleep: Int, heartRate: Int, notes: String) {
        viewModelScope.launch {
            if (!UserSession.hasActiveSession()) return@launch
            val userId = UserSession.state.value.userId ?: return@launch
            val token = UserSession.bearerTokenOrEmpty()

            val desc = "Estrés: $stress/10, Sueño: ${sleep}h, Ritmo: ${heartRate}bpm. $notes".trim()

            // Enviar trigger de estrés al endpoint POST /triggers del backend
            val triggerReq = com.example.neurozen_front.neurozen.data.network.TriggerRequest(
                userId = userId,
                stressLevel = stress,
                triggerSource = "Check-in Diario",
                description = desc
            )
            repository.createTrigger(triggerReq, token)

            refresh()
            loadHealthHistory()
        }
    }

    fun completeSession(sessionId: String) {
        _homeState.update { current ->
            current.copy(
                sessions = current.sessions.map {
                    if (it.id == sessionId) it.copy(status = "Completada") else it
                }
            )
        }
    }

    private fun formatNow(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    private fun parseIsoDate(dateStr: String): Long {
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
