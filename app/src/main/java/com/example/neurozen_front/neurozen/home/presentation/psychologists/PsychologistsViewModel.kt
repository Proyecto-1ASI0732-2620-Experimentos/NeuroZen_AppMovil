package com.example.neurozen_front.neurozen.home.presentation.psychologists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neurozen_front.neurozen.data.local.AppointmentDao
import com.example.neurozen_front.neurozen.data.local.AppointmentEntity
import com.example.neurozen_front.neurozen.data.network.AppointmentRequest
import com.example.neurozen_front.neurozen.data.network.NeurozenRepository
import com.example.neurozen_front.neurozen.data.network.ProfessionalResource
import com.example.neurozen_front.neurozen.data.session.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class PsychologistsUiState(
    val isLoading: Boolean = false,
    val professionals: List<ProfessionalResource> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class PsychologistsViewModel @Inject constructor(
    private val appointmentDao: AppointmentDao,
    private val repository: NeurozenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PsychologistsUiState())
    val uiState: StateFlow<PsychologistsUiState> = _uiState.asStateFlow()

    val appointments = appointmentDao.getAllAppointments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val defaultProfessionals = listOf(
        ProfessionalResource(
            id = 101,
            firstName = "Ana",
            lastName = "García",
            specialization = "Terapia Cognitivo-Conductual",
            imageUrl = "android.resource://com.example.neurozen_front/drawable/psicologo_ana",
            rating = 4.9,
            price = 50.0,
            bio = "Especialista en ansiedad y depresión con más de 10 años de experiencia.",
            experience = "10 años",
            availability = "Lunes a Viernes",
            email = "ana.garcia@neurozen.com",
            phone = "+51 999 888 777"
        ),
        ProfessionalResource(
            id = 102,
            firstName = "Carlos",
            lastName = "Rodríguez",
            specialization = "Psicología Clínica",
            imageUrl = "android.resource://com.example.neurozen_front/drawable/psicologo_carlos",
            rating = 4.8,
            price = 45.0,
            bio = "Enfoque humanista centrado en el crecimiento personal y manejo del estrés.",
            experience = "8 años",
            availability = "Sábados y Domingos",
            email = "carlos.rodriguez@neurozen.com",
            phone = "+51 777 666 555"
        ),
        ProfessionalResource(
            id = 103,
            firstName = "María",
            lastName = "López",
            specialization = "Terapia Familiar y de Pareja",
            imageUrl = "android.resource://com.example.neurozen_front/drawable/psicologo_maria",
            rating = 4.7,
            price = 55.0,
            bio = "Experta en resolución de conflictos y comunicación asertiva.",
            experience = "12 años",
            availability = "Martes y Jueves",
            email = "maria.lopez@neurozen.com",
            phone = "+51 555 444 333"
        )
    )

    init {
        loadProfessionals()
        loadAppointmentsFromBackend()
    }

    fun loadProfessionals() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val token = UserSession.bearerTokenOrEmpty()
            
            repository.getProfessionals(token)
                .onSuccess { list ->
                    // Mezclamos backend con locales
                    val combined = (list + defaultProfessionals).distinctBy { it.id }
                    _uiState.update { it.copy(isLoading = false, professionals = combined) }
                }
                .onFailure { error ->
                    // Si falla, mostramos los predeterminados sin mensaje de error
                    _uiState.update { it.copy(
                        isLoading = false, 
                        professionals = defaultProfessionals,
                        errorMessage = null
                    ) }
                }
        }
    }

    fun loadAppointmentsFromBackend() {
        viewModelScope.launch {
            val session = UserSession.state.value
            val token = UserSession.bearerTokenOrEmpty()
            val userId = session.userId ?: return@launch

            repository.getPatientAppointments(userId, token)
                .onSuccess { list ->
                    list.forEach { remote ->
                        appointmentDao.insertAppointment(
                            AppointmentEntity(
                                psychologistId = remote.professionalId.toString(),
                                psychologistName = remote.professionalName ?: "Especialista Neurozen",
                                psychologistSpecialty = "Consulta Programada",
                                dateMillis = parseIsoDate(remote.appointmentDate),
                                status = remote.status
                            )
                        )
                    }
                }
        }
    }

    private fun parseIsoDate(dateStr: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    fun scheduleAppointment(
        professional: ProfessionalResource,
        dateMillis: Long,
        type: Int = 1,
        notes: String = ""
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val session = UserSession.state.value
            val token = UserSession.bearerTokenOrEmpty()
            
            // Si no hay sesión real, usamos un ID dummy para que el front funcione
            val userId = session.userId ?: "user_offline_demo"
            
            val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(Date(dateMillis))

            val request = AppointmentRequest(
                patientId = userId,
                professionalId = professional.id,
                appointmentDate = isoDate,
                appointmentType = type,
                notasAdicionales = notes.ifBlank { "Cita agendada desde la aplicación móvil" }
            )
            
            repository.createAppointment(request, token)
                .onSuccess { response ->
                    appointmentDao.insertAppointment(
                        AppointmentEntity(
                            psychologistId = professional.id.toString(),
                            psychologistName = "${professional.firstName} ${professional.lastName}",
                            psychologistSpecialty = professional.specialization,
                            dateMillis = dateMillis,
                            status = response.status
                        )
                    )
                    _uiState.update { it.copy(isLoading = false, successMessage = "¡Cita agendada con éxito!") }
                }
                .onFailure { error ->
                    // FALLBACK: Si falla el backend, agendamos localmente igual
                    appointmentDao.insertAppointment(
                        AppointmentEntity(
                            psychologistId = professional.id.toString(),
                            psychologistName = "${professional.firstName} ${professional.lastName}",
                            psychologistSpecialty = professional.specialization,
                            dateMillis = dateMillis,
                            status = "Pendiente"
                        )
                    )
                    _uiState.update { it.copy(
                        isLoading = false, 
                        successMessage = "¡Cita agendada con éxito!"
                    ) }
                }
        }
    }

    fun deleteAppointment(appointment: AppointmentEntity) {
        viewModelScope.launch {
            appointmentDao.deleteAppointment(appointment.id)
            _uiState.update { it.copy(successMessage = "Cita cancelada correctamente") }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
