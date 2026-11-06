package com.example.neurozen_front.presentation.psychologist

import androidx.compose.foundation.background
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.Color
import com.example.neurozen_front.presentation.components.Accent
import com.example.neurozen_front.presentation.components.Avatar
import com.example.neurozen_front.presentation.components.CompletionCard
import com.example.neurozen_front.presentation.components.DateBlock
import com.example.neurozen_front.presentation.components.ErrorBanner
import com.example.neurozen_front.presentation.components.FeatureTile
import com.example.neurozen_front.presentation.components.GradientHeader
import com.example.neurozen_front.presentation.components.GroupDivider
import com.example.neurozen_front.presentation.components.HeaderStat
import com.example.neurozen_front.presentation.components.IconBadge
import com.example.neurozen_front.presentation.components.ListRow
import com.example.neurozen_front.presentation.components.SectionHeader
import com.example.neurozen_front.presentation.components.SettingsGroup
import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Verified
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.unit.sp
import com.example.neurozen_front.presentation.components.EmojiCircle
import com.example.neurozen_front.presentation.components.StatBadge
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neurozen_front.domain.logic.AppointmentRules
import com.example.neurozen_front.domain.logic.Catalogs
import com.example.neurozen_front.domain.logic.DateTimeUtils
import com.example.neurozen_front.domain.logic.DayCodec
import com.example.neurozen_front.domain.logic.Validators
import com.example.neurozen_front.domain.model.Appointment
import com.example.neurozen_front.domain.model.AppointmentStatus
import com.example.neurozen_front.domain.model.AvailabilitySchedule
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.ProfessionalProfile
import com.example.neurozen_front.domain.model.Report
import com.example.neurozen_front.domain.model.Role
import com.example.neurozen_front.domain.repository.AppointmentRepository
import com.example.neurozen_front.domain.repository.ReportRepository
import com.example.neurozen_front.domain.repository.UserRepository
import com.example.neurozen_front.domain.usecase.LogoutUseCase
import com.example.neurozen_front.notifications.ReminderScheduler
import com.example.neurozen_front.presentation.components.ConfirmDialog
import com.example.neurozen_front.presentation.components.MessageChannel
import com.example.neurozen_front.presentation.components.NzTextField
import com.example.neurozen_front.presentation.components.NzTopBar
import com.example.neurozen_front.presentation.components.Pill
import com.example.neurozen_front.presentation.components.PrimaryButton
import com.example.neurozen_front.presentation.components.SectionCard
import com.example.neurozen_front.presentation.components.StateContent
import com.example.neurozen_front.presentation.components.StatusPill
import com.example.neurozen_front.presentation.components.UiState
import com.example.neurozen_front.presentation.components.toUiState
import com.example.neurozen_front.presentation.theme.NzColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/** Appointments of the logged-in psychologist + status actions (confirm, cancel, mark as attended). */
@HiltViewModel
class ProAgendaViewModel @Inject constructor(
    private val appointments: AppointmentRepository,
    private val reminders: ReminderScheduler
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Appointment>>>(UiState.Loading)
    val state = _state.asStateFlow()
    val messages = MessageChannel()
    private var professionalId = 0L

    fun start(id: Long) {
        if (professionalId == id && _state.value !is UiState.Error) return
        professionalId = id
        load()
    }

    fun load() = viewModelScope.launch {
        _state.value = UiState.Loading
        val r = appointments.getForProfessional(professionalId)
        if (r is Outcome.Success) reminders.syncAppointments(r.data)
        _state.value = r.toUiState()
    }

    fun changeStatus(a: Appointment, target: AppointmentStatus) {
        if (!AppointmentRules.canTransition(a.status, target, Role.PSYCHOLOGIST)) { messages.send("Cambio de estado no permitido"); return }
        viewModelScope.launch {
            when (val r = appointments.updateStatus(a.id, target)) {
                is Outcome.Success -> {
                    messages.send(when (target) {
                        AppointmentStatus.CONFIRMED -> "Cita confirmada"
                        AppointmentStatus.COMPLETED -> "Cita marcada como atendida"
                        else -> "Cita cancelada"
                    })
                    load()
                }
                is Outcome.Failure -> messages.send(r.error.message)
            }
        }
    }
}

@HiltViewModel
class ProReportsViewModel @Inject constructor(private val reports: ReportRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Report>>>(UiState.Loading)
    val state = _state.asStateFlow()
    private var professionalId = 0L
    fun start(id: Long) { if (professionalId != id) { professionalId = id; load() } }
    fun load() = viewModelScope.launch { _state.value = UiState.Loading; _state.value = reports.getReportsForProfessional(professionalId).toUiState() }
}

@Composable
private fun AppointmentActions(a: Appointment, vm: ProAgendaViewModel) {
    var confirmCancel by remember { mutableStateOf(false) }
    val allowed = AppointmentRules.allowedTransitions(a.status, Role.PSYCHOLOGIST)
    if (allowed.isEmpty()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (AppointmentStatus.CONFIRMED in allowed) androidx.compose.material3.Button(onClick = { vm.changeStatus(a, AppointmentStatus.CONFIRMED) }) { Text("Confirmar") }
        if (AppointmentStatus.COMPLETED in allowed && a.dateTime.isBefore(Instant.now())) {
            OutlinedButton(onClick = { vm.changeStatus(a, AppointmentStatus.COMPLETED) }) { Text("Atendida") }
        }
        Spacer(Modifier.weight(1f))
        if (AppointmentStatus.CANCELLED in allowed) TextButton(onClick = { confirmCancel = true }) { Text("Cancelar", color = MaterialTheme.colorScheme.error) }
    }
    if (confirmCancel) ConfirmDialog("Cancelar cita", "Se avisará al paciente en su app.", "Cancelar cita", { vm.changeStatus(a, AppointmentStatus.CANCELLED) }, { confirmCancel = false }, destructive = true)
}

@Composable
private fun AppointmentRow(a: Appointment, vm: ProAgendaViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                DateBlock(a.dateTime, highlighted = a.status == AppointmentStatus.CONFIRMED || a.status == AppointmentStatus.PENDING)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Avatar(a.patientName ?: "Paciente", 28)
                        Text(a.patientName ?: "Paciente #${a.patientId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    a.notes?.takeIf { it.isNotBlank() }?.let {
                        Text("Motivo: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                    Spacer(Modifier.height(2.dp))
                    StatusPill(a.status)
                }
            }
            if (AppointmentRules.allowedTransitions(a.status, Role.PSYCHOLOGIST).isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AppointmentActions(a, vm)
            }
        }
    }
}

@Composable
private fun Messages(vm: ProAgendaViewModel, snackbar: SnackbarHostState) {
    LaunchedEffect(vm) { vm.messages.messages.collect { snackbar.showSnackbar(it) } }
}

// ---------- Home ----------

@Composable
fun ProHomeScreen(profile: ProfessionalProfile, onOpenSchedule: () -> Unit, onEditProfile: () -> Unit, vm: ProAgendaViewModel = hiltViewModel()) {
    LaunchedEffect(profile.id) { vm.start(profile.id) }
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    Messages(vm, snackbar)
    val now = Instant.now()
    val today = DateTimeUtils.todayInLima()
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        val all = (state as? UiState.Success)?.data.orEmpty()
        val todayCount = all.count { DateTimeUtils.isSameLimaDay(it.dateTime, today) && it.status != AppointmentStatus.CANCELLED }
        val pendingCount = all.count { it.status == AppointmentStatus.PENDING && it.dateTime.isAfter(now) }
        val patientsCount = all.filter { it.status != AppointmentStatus.CANCELLED }.map { it.patientId }.distinct().size
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                GradientHeader(
                    title = "Hola, ${profile.name.substringBefore(' ')}",
                    subtitle = "Portal del Psicólogo · ${DateTimeUtils.formatDate(today)}",
                    trailing = { Avatar(profile.name, 56, ring = true) },
                    bottom = {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(" Psicólogo verificado · CPsP ${profile.licenseNumber}", style = MaterialTheme.typography.labelMedium, color = Color.White)
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HeaderStat(if (state is UiState.Success || state is UiState.Empty) todayCount.toString() else "—", "Hoy", Modifier.weight(1f))
                            HeaderStat(if (state is UiState.Success || state is UiState.Empty) pendingCount.toString() else "—", "Por confirmar", Modifier.weight(1f))
                            HeaderStat(if (state is UiState.Success || state is UiState.Empty) patientsCount.toString() else "—", "Pacientes", Modifier.weight(1f))
                            HeaderStat("★ ${"%.1f".format(profile.rating)}", "${profile.reviews} reseñas", Modifier.weight(1f))
                        }
                    }
                )
            }
            val steps = profile.completedSteps
            if (steps.any { !it }) item {
                CompletionCard(
                    "Completa tu perfil profesional", steps.count { it }, 4,
                    "Biografía, experiencia, tarifa y horario ayudan a que los pacientes te elijan.",
                    Modifier.padding(horizontal = 20.dp), onEditProfile
                )
            }
            item {
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureTile(Icons.Outlined.CalendarMonth, "Agenda", "Semana y disponibilidad", Accent.Emerald, Modifier.weight(1f), onOpenSchedule)
                    FeatureTile(Icons.Outlined.Person, "Mi perfil", "Bio, tarifa y datos", Accent.Blue, Modifier.weight(1f), onEditProfile)
                }
            }
            when (val s = state) {
                UiState.Loading -> item { Text("Cargando citas…", modifier = Modifier.padding(horizontal = 20.dp)) }
                is UiState.Error -> item { Column(Modifier.padding(horizontal = 20.dp)) { ErrorBanner(s.message); TextButton(onClick = { vm.load() }) { Text("Reintentar") } } }
                UiState.Empty -> item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            IconBadge(Icons.Outlined.CalendarMonth, size = 56)
                            Spacer(Modifier.height(12.dp))
                            Text("No tienes citas por ahora", fontWeight = FontWeight.SemiBold)
                            Text("Cuando un paciente agende una cita contigo, aparecerá aquí automáticamente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
                is UiState.Success -> {
                    val list = s.data
                    val todayList = list.filter { DateTimeUtils.isSameLimaDay(it.dateTime, today) && it.status != AppointmentStatus.CANCELLED }
                    val pending = list.filter { it.status == AppointmentStatus.PENDING && it.dateTime.isAfter(now) }
                    val upcoming = list.filter { it.status == AppointmentStatus.CONFIRMED && it.dateTime.isAfter(now) }.take(5)
                    val activePatients = list.filter { it.status != AppointmentStatus.CANCELLED }.map { it.patientId }.distinct().size
                    item { SectionHeader("Citas de hoy", Modifier.padding(horizontal = 20.dp)) }
                    if (todayList.isEmpty()) item { Text("No tienes citas hoy.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp)) }
                    items(todayList, key = { "t${it.id}" }) { Box(Modifier.padding(horizontal = 20.dp)) { AppointmentRow(it, vm) } }
                    item { SectionHeader("Por confirmar", Modifier.padding(horizontal = 20.dp)) }
                    if (pending.isEmpty()) item { Text("Nada pendiente por confirmar.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp)) }
                    items(pending, key = { "p${it.id}" }) { Box(Modifier.padding(horizontal = 20.dp)) { AppointmentRow(it, vm) } }
                    item { SectionHeader("Próximas confirmadas", Modifier.padding(horizontal = 20.dp), "Ver agenda", onOpenSchedule) }
                    items(upcoming, key = { "u${it.id}" }) { Box(Modifier.padding(horizontal = 20.dp)) { AppointmentRow(it, vm) } }
                }
            }
        }
    }
}

// ---------- Patients (derived from appointments) ----------

data class PatientSummary(val id: Long, val name: String, val email: String?, val sessions: Int, val last: Instant?)

@Composable
fun PatientsScreen(profile: ProfessionalProfile, onOpen: (Long) -> Unit, vm: ProAgendaViewModel = hiltViewModel()) {
    LaunchedEffect(profile.id) { vm.start(profile.id) }
    val state by vm.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        GradientHeader("Pacientes", "Personas que reservaron sesiones contigo")
        StateContent(state, onRetry = { vm.load() }, emptyMessage = "Aún no tienes pacientes.") { list ->
            val patients = list.groupBy { it.patientId }.map { (id, appts) ->
                PatientSummary(id, appts.firstNotNullOfOrNull { it.patientName } ?: "Paciente #$id", appts.firstNotNullOfOrNull { it.patientEmail },
                    appts.count { it.status == AppointmentStatus.COMPLETED }, appts.maxOfOrNull { it.dateTime })
            }.filter { query.isBlank() || it.name.contains(query, true) || it.email?.contains(query, true) == true }.sortedBy { it.name }
            LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { NzTextField(query, { query = it }, "Buscar paciente") }
                items(patients, key = { it.id }) { p ->
                    SectionCard(Modifier.clickable { onOpen(p.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Avatar(p.name, 48)
                            Column(Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                p.email?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                Text(p.last?.let { "Última cita: ${DateTimeUtils.formatDateTime(it)}" } ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(p.sessions.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("atendidas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecureScreen() {
    val activity = LocalContext.current as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

@Composable
fun PatientDetailScreen(
    profile: ProfessionalProfile,
    patientId: Long,
    onBack: () -> Unit,
    agenda: ProAgendaViewModel = hiltViewModel(),
    reportsVm: ProReportsViewModel = hiltViewModel()
) {
    SecureScreen()
    LaunchedEffect(profile.id) { agenda.start(profile.id); reportsVm.start(profile.id) }
    val appts by agenda.state.collectAsState()
    val reports by reportsVm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    Messages(agenda, snackbar)
    val history = (appts as? UiState.Success)?.data.orEmpty().filter { it.patientId == patientId }.sortedByDescending { it.dateTime }
    val patientReports = (reports as? UiState.Success)?.data.orEmpty().filter { it.patientId == patientId }
    val name = history.firstNotNullOfOrNull { it.patientName } ?: patientReports.firstNotNullOfOrNull { it.patientName } ?: "Paciente"
    Scaffold(topBar = { NzTopBar(name, onBack) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            history.firstNotNullOfOrNull { it.patientEmail }?.let { email -> item { Text(email, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            item { Text("Informes compartidos (${patientReports.size})", style = MaterialTheme.typography.titleMedium) }
            if (patientReports.isEmpty()) item { Text("El paciente aún no compartió informes.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(patientReports, key = { "r${it.id}" }) { ReportCard(it) }
            item { Text("Historial de citas", style = MaterialTheme.typography.titleMedium) }
            items(history, key = { "a${it.id}" }) { AppointmentRow(it, agenda) }
        }
    }
}

// ---------- Weekly schedule + availability ----------

@Composable
fun ScheduleScreen(profile: ProfessionalProfile, root: PsychologistViewModel, vm: ProAgendaViewModel = hiltViewModel()) {
    LaunchedEffect(profile.id) { vm.start(profile.id) }
    val state by vm.state.collectAsState()
    val saving by root.saving.collectAsState()
    val rootMessage by root.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    Messages(vm, snackbar)
    LaunchedEffect(rootMessage) { rootMessage?.let { snackbar.showSnackbar(it); root.clearMessage() } }

    var weekStartEpoch by rememberSaveable { mutableLongStateOf(DateTimeUtils.todayInLima().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()) }
    val weekStart = LocalDate.ofEpochDay(weekStartEpoch)
    val style = DayCodec.detectStyle(profile.schedule?.days.orEmpty())
    var days by rememberSaveable(profile.schedule) { mutableStateOf(profile.schedule?.days.orEmpty().mapNotNull(DayCodec::parse).map { it.name }.toSet()) }
    var start by rememberSaveable(profile.schedule) { mutableStateOf(profile.schedule?.start?.ifBlank { null } ?: "09:00") }
    var end by rememberSaveable(profile.schedule) { mutableStateOf(profile.schedule?.end?.ifBlank { null } ?: "18:00") }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
      Column(Modifier.padding(padding)) {
        GradientHeader("Agenda", "Confirma, atiende y organiza tu semana")
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { weekStartEpoch -= 7 }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Semana anterior") }
                    Text("Semana del ${DateTimeUtils.formatDate(weekStart)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { weekStartEpoch += 7 }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Semana siguiente") }
                }
            }
            when (val s = state) {
                UiState.Loading -> item { Text("Cargando citas…") }
                is UiState.Error -> item { Text(s.message, color = MaterialTheme.colorScheme.error); TextButton(onClick = { vm.load() }) { Text("Reintentar") } }
                UiState.Empty -> item { Text("Sin citas registradas.") }
                is UiState.Success -> (0..6).forEach { offset ->
                    val day = weekStart.plusDays(offset.toLong())
                    val dayAppointments = s.data.filter { DateTimeUtils.isSameLimaDay(it.dateTime, day) }.sortedBy { it.dateTime }
                    item(key = "h$offset") {
                        Text(DateTimeUtils.formatDate(day), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                        if (dayAppointments.isEmpty()) Text("Sin citas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(dayAppointments, key = { "s${it.id}" }) { AppointmentRow(it, vm) }
                }
            }
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            item {
                SectionCard {
                    Text("Disponibilidad semanal", style = MaterialTheme.typography.titleMedium)
                    Text("Los pacientes verán bloques de 1 hora dentro de este horario (hora de Lima).", style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(DayOfWeek.entries.toList()) { d ->
                            FilterChip(selected = d.name in days, onClick = { days = if (d.name in days) days - d.name else days + d.name }, label = { Text(DateTimeUtils.dayShort(d)) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NzTextField(start, { start = it }, "Desde", Modifier.weight(1f), error = if (Validators.isValidHour(start)) null else "HH:mm")
                        NzTextField(end, { end = it }, "Hasta", Modifier.weight(1f), error = if (Validators.isValidHour(end)) null else "HH:mm")
                    }
                    val valid = days.isNotEmpty() && Validators.isValidHour(start) && Validators.isValidHour(end) && end > start
                    if (!valid) Text("Elige días y un horario válido (fin posterior al inicio).", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    PrimaryButton("Guardar disponibilidad", onClick = {
                        val selected = days.map { DayOfWeek.valueOf(it) }.toSet()
                        val schedule = AvailabilitySchedule(selected.sorted().map { DayCodec.encode(it, style) }, start, end)
                        root.save({ it.copy(schedule = schedule, availability = DayCodec.summary(selected, start, end)) }, "Disponibilidad actualizada")
                    }, loading = saving, enabled = valid)
                }
            }
        }
      }
    }
}

// ---------- Reports received (US15) ----------

@Composable
fun ReportCard(r: Report) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(r.patientName ?: "Paciente", 40)
            Column(Modifier.weight(1f)) {
                Text(r.patientName ?: "Paciente #${r.patientId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                r.createdAt?.let { Text("Compartido el ${DateTimeUtils.formatDateTime(it)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Pill("Informe", MaterialTheme.colorScheme.secondary)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportMetric("Actual", "%.0f/10".format(r.stats.current), Modifier.weight(1f))
            ReportMetric("Promedio", "%.1f".format(r.stats.average), Modifier.weight(1f))
            ReportMetric("Semana", "%+.1f".format(r.stats.weeklyChange), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportMetric("Hora pico", r.stats.peakHours.substringBefore(" "), Modifier.weight(1f))
            ReportMetric("Registros", r.stats.triggersCount.toString(), Modifier.weight(1f))
            ReportMetric("Último test", r.stats.lastAssessment?.let { "${it.score}/40" } ?: "—", Modifier.weight(1f))
        }
        Text("Principal fuente de estrés: ${Catalogs.categoryLabel(r.stats.topCategory)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        r.note?.takeIf { it.isNotBlank() }?.let {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                Text("“$it”", style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
private fun ReportMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)).padding(10.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ReportsScreen(profile: ProfessionalProfile, vm: ProReportsViewModel = hiltViewModel()) {
    SecureScreen()
    LaunchedEffect(profile.id) { vm.start(profile.id) }
    val state by vm.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        GradientHeader("Informes recibidos", "Progreso que tus pacientes compartieron contigo")
        StateContent(state, onRetry = { vm.load() }, emptyMessage = "Cuando un paciente comparta su informe de progreso aparecerá aquí.") { list ->
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { ReportCard(it) }
            }
        }
    }
}

// ---------- Professional profile ----------

@HiltViewModel
class ProAccountViewModel @Inject constructor(private val users: UserRepository, private val logout: LogoutUseCase) : ViewModel() {
    val messages = MessageChannel()
    fun deleteAccount() = viewModelScope.launch {
        when (val r = users.deleteMe()) {
            is Outcome.Success -> logout()
            is Outcome.Failure -> messages.send(r.error.message)
        }
    }
}

@Composable
fun ProProfileScreen(
    profile: ProfessionalProfile,
    root: PsychologistViewModel,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    onOpenSchedule: () -> Unit = {},
    account: ProAccountViewModel = hiltViewModel()
) {
    val saving by root.saving.collectAsState()
    val message by root.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); root.clearMessage() } }
    LaunchedEffect(Unit) { account.messages.messages.collect { snackbar.showSnackbar(it) } }
    var bio by rememberSaveable(profile.bio) { mutableStateOf(profile.bio) }
    var specialty by rememberSaveable(profile.specialty) { mutableStateOf(profile.specialty) }
    var years by rememberSaveable(profile.experienceYears) { mutableStateOf(profile.experienceYears?.toString().orEmpty()) }
    var price by rememberSaveable(profile.price) { mutableStateOf(profile.price?.let { "%.2f".format(java.util.Locale.US, it) }.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val steps = profile.completedSteps

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                GradientHeader(
                    title = "Mi perfil profesional", subtitle = "Así te ven los pacientes en el directorio",
                    bottom = {
                        Spacer(Modifier.height(18.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Avatar(profile.name, 76, ring = true)
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(profile.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f, fill = false))
                                    Icon(Icons.Filled.Verified, contentDescription = "Verificado", tint = Color.White, modifier = Modifier.padding(start = 6.dp).size(20.dp))
                                }
                                Text(profile.specialty, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                                Text("CPsP ${profile.licenseNumber} · ${profile.email}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HeaderStat("★ ${"%.1f".format(profile.rating)}", "Valoración", Modifier.weight(1f))
                            HeaderStat(profile.reviews.toString(), "Reseñas", Modifier.weight(1f))
                            HeaderStat(profile.experienceYears?.let { "$it años" } ?: "—", "Experiencia", Modifier.weight(1f))
                            HeaderStat(profile.price?.let { "S/ ${"%.0f".format(it)}" } ?: "—", "Tarifa", Modifier.weight(1f))
                        }
                    }
                )
            }
            if (steps.any { !it }) item {
                CompletionCard(
                    "Perfil completado", steps.count { it }, 4,
                    "Completa biografía, experiencia, tarifa y horario para destacar en el directorio.",
                    Modifier.padding(horizontal = 20.dp)
                )
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        "PERFIL PÚBLICO", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                    )
                    SectionCard {
                        NzTextField(specialty, { specialty = it }, "Especialidad")
                        NzTextField(bio, { bio = it.take(1000) }, "Biografía", singleLine = false, minLines = 4, supporting = "${bio.length}/1000 · Cuéntales tu enfoque y con qué temas trabajas")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NzTextField(years, { years = it.filter(Char::isDigit).take(2) }, "Años de experiencia", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                            NzTextField(price, { price = it.replace(',', '.') }, "Tarifa (S/)", Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                        }
                        val priceValue = price.toDoubleOrNull()
                        val valid = specialty.isNotBlank() && (price.isBlank() || (priceValue != null && priceValue >= 0))
                        PrimaryButton("Guardar cambios", onClick = {
                            root.save({ it.copy(specialty = specialty, bio = bio, experienceYears = years.toIntOrNull(), price = priceValue) }, "Perfil actualizado")
                        }, loading = saving, enabled = valid)
                    }
                }
            }
            item {
                SettingsGroup("Datos verificados", Modifier.padding(horizontal = 20.dp)) {
                    ListRow(Icons.Outlined.Badge, "Nombre completo", profile.name, showChevron = false)
                    GroupDivider()
                    ListRow(Icons.Outlined.VerifiedUser, "Colegiatura CPsP", "N.° ${profile.licenseNumber} · ${profile.status.label}", tint = Accent.Emerald, showChevron = false)
                    GroupDivider()
                    ListRow(Icons.Outlined.Info, "¿Necesitas cambiarlos?", "Modificar nombre o colegiatura requiere una nueva verificación del administrador.", tint = Accent.Blue, showChevron = false)
                }
            }
            item {
                SettingsGroup("Atención", Modifier.padding(horizontal = 20.dp)) {
                    ListRow(Icons.Outlined.Schedule, "Horario de atención", profile.availability.ifBlank { "Aún no configurado" }, tint = Accent.Teal, onClick = onOpenSchedule)
                    GroupDivider()
                    ListRow(Icons.Outlined.Payments, "Tarifa por sesión", trailingText = profile.price?.let { "S/ ${"%.2f".format(it)}" } ?: "Sin definir", tint = Accent.Amber, showChevron = false)
                }
            }
            item {
                SettingsGroup("Cuenta y seguridad", Modifier.padding(horizontal = 20.dp)) {
                    ListRow(Icons.Outlined.Lock, "Cambiar contraseña", onClick = onChangePassword)
                    GroupDivider()
                    ListRow(Icons.Outlined.Shield, "Confidencialidad", "Los informes y fichas de pacientes no permiten capturas de pantalla", showChevron = false)
                    GroupDivider()
                    ListRow(Icons.AutoMirrored.Outlined.Logout, "Cerrar sesión", showChevron = false) { confirmLogout = true }
                    GroupDivider()
                    ListRow(Icons.Outlined.DeleteForever, "Eliminar mi cuenta", destructive = true, showChevron = false) { confirmDelete = true }
                }
            }
            item {
                Text(
                    "NeuroZen para Psicólogos · v${com.example.neurozen_front.BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    if (confirmLogout) ConfirmDialog("Cerrar sesión", "Se borrarán los datos guardados en este teléfono.", "Cerrar sesión", onLogout, { confirmLogout = false })
    if (confirmDelete) ConfirmDialog("Eliminar cuenta", "Se eliminarán tu cuenta y tus datos personales de forma permanente (Ley 29733).", "Eliminar", account::deleteAccount, { confirmDelete = false }, destructive = true)
}
