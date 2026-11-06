package com.example.neurozen_front.presentation.worker

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import com.example.neurozen_front.presentation.components.GradientHeader
import com.example.neurozen_front.presentation.components.HeaderStat
import com.example.neurozen_front.presentation.components.IconBadge
import com.example.neurozen_front.presentation.components.SectionHeader
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.example.neurozen_front.presentation.components.Avatar
import com.example.neurozen_front.presentation.components.Tag
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.example.neurozen_front.domain.logic.DateTimeUtils
import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.AppointmentType
import com.example.neurozen_front.domain.model.BookingRequest
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.Professional
import com.example.neurozen_front.domain.model.Review
import com.example.neurozen_front.domain.model.getOrNull
import com.example.neurozen_front.domain.repository.AppointmentRepository
import com.example.neurozen_front.domain.repository.ProfessionalRepository
import com.example.neurozen_front.domain.usecase.BookAppointmentUseCase
import com.example.neurozen_front.presentation.components.MessageChannel
import com.example.neurozen_front.presentation.components.NzTextField
import com.example.neurozen_front.presentation.components.NzTopBar
import com.example.neurozen_front.presentation.components.PrimaryButton
import com.example.neurozen_front.presentation.components.SectionCard
import com.example.neurozen_front.presentation.components.StateContent
import com.example.neurozen_front.presentation.components.UiState
import com.example.neurozen_front.presentation.components.toUiState
import com.example.neurozen_front.presentation.theme.NzColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// ---------- Directory (only verified professionals come from the backend) ----------

@HiltViewModel
class ProfessionalsViewModel @Inject constructor(private val repo: ProfessionalRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Professional>>>(UiState.Loading)
    val state = _state.asStateFlow()
    init { load() }
    fun load() = viewModelScope.launch { _state.value = UiState.Loading; _state.value = repo.getDirectory().toUiState() }
}

@Composable
fun ProfessionalsScreen(onOpen: (Long) -> Unit, vm: ProfessionalsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var specialty by rememberSaveable { mutableStateOf<String?>(null) }
    StateContent(state, onRetry = { vm.load() }, emptyMessage = "Todavía no hay psicólogos verificados disponibles.") { all ->
        val specialties = all.map { it.specialty }.filter { it.isNotBlank() }.distinct().sorted()
        val filtered = all.filter {
            (specialty == null || it.specialty == specialty) && (query.isBlank() || it.name.contains(query, true) || it.specialty.contains(query, true))
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { GradientHeader("Centro de Ayuda", "Profesionales verificados por el CPsP, listos para escucharte") }
            item { NzTextField(query, { query = it }, "Buscar por nombre o especialidad", Modifier.padding(horizontal = 16.dp)) }
            if (specialties.isNotEmpty()) item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(specialties) { s -> FilterChip(selected = specialty == s, onClick = { specialty = if (specialty == s) null else s }, label = { Text(s) }) }
                }
            }
            if (filtered.isEmpty()) item { Text("Sin resultados.", Modifier.padding(horizontal = 16.dp)) }
            items(filtered, key = { it.id }) { p -> Box(Modifier.padding(horizontal = 16.dp)) { ProfessionalCard(p) { onOpen(p.id) } } }
        }
    }
}

@Composable
private fun ProfessionalCard(p: Professional, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Avatar(p.name, 60, p.imageUrl)
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                        Icon(Icons.Filled.Verified, contentDescription = "Verificado por CPsP", tint = NzColors.Turquoise, modifier = Modifier.padding(start = 4.dp).size(18.dp))
                    }
                    Text(p.specialty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = NzColors.Moderate, modifier = Modifier.size(16.dp))
                        Text(" ${"%.1f".format(p.rating)}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("  (${p.reviews} reseñas)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (p.bio.isNotBlank()) Text(p.bio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                p.experienceYears?.let { Tag("$it años exp.") }
                Tag("CPsP verificado")
                Box(Modifier.weight(1f))
                p.price?.let { Text("S/ ${"%.0f".format(it)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

// ---------- Detail + reviews ----------

data class ProfessionalDetail(val professional: Professional, val reviews: List<Review>)

@HiltViewModel
class ProfessionalDetailViewModel @Inject constructor(savedState: SavedStateHandle, private val repo: ProfessionalRepository) : ViewModel() {
    private val id: Long = savedState["id"] ?: 0L
    private val _state = MutableStateFlow<UiState<ProfessionalDetail>>(UiState.Loading)
    val state = _state.asStateFlow()
    init { load() }
    fun load() = viewModelScope.launch {
        _state.value = UiState.Loading
        val reviews = async { repo.getReviews(id) }
        _state.value = when (val p = repo.getById(id)) {
            is Outcome.Success -> UiState.Success(ProfessionalDetail(p.data, reviews.await().getOrNull().orEmpty()))
            is Outcome.Failure -> UiState.Error(p.error.message)
        }
    }
}

@Composable
fun ProfessionalDetailScreen(onBack: () -> Unit, onBook: () -> Unit, vm: ProfessionalDetailViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    Scaffold(
        topBar = { NzTopBar("Perfil profesional", onBack) },
        bottomBar = {
            if (state is UiState.Success) {
                Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        val price = (state as UiState.Success<ProfessionalDetail>).data.professional.price
                        Column(Modifier.weight(1f)) {
                            Text("Tarifa por sesión", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(price?.let { "S/ ${"%.2f".format(it)}" } ?: "Consultar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        PrimaryButton("Reservar sesión", onBook, Modifier.weight(1.3f))
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            StateContent(state, onRetry = { vm.load() }) { (p, reviews) ->
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        GradientHeader(
                            title = p.name, subtitle = p.specialty,
                            trailing = { Avatar(p.name, 72, p.imageUrl, ring = true) },
                            bottom = {
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text(" Colegiatura verificada por el CPsP", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                }
                                Spacer(Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    HeaderStat("★ ${"%.1f".format(p.rating)}", "Valoración", Modifier.weight(1f))
                                    HeaderStat(p.reviews.toString(), "Reseñas", Modifier.weight(1f))
                                    HeaderStat(p.experienceYears?.let { "$it años" } ?: "—", "Experiencia", Modifier.weight(1f))
                                }
                            }
                        )
                    }
                    item {
                        SectionCard(Modifier.padding(horizontal = 20.dp)) {
                            SectionHeader("Sobre mí")
                            Text(p.bio.ifBlank { "Este profesional aún no agregó su biografía." }, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (p.availability.isNotBlank()) item {
                        SectionCard(Modifier.padding(horizontal = 20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconBadge(Icons.Outlined.Schedule)
                                Column {
                                    Text("Horario de atención", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(p.availability, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    item { SectionHeader("Reseñas de pacientes (${reviews.size})", Modifier.padding(horizontal = 20.dp)) }
                    if (reviews.isEmpty()) item {
                        Text("Aún no tiene reseñas.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
                    }
                    items(reviews, key = { it.id }) { r ->
                        SectionCard(Modifier.padding(horizontal = 20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Avatar(r.userName, 38)
                                Column(Modifier.weight(1f)) {
                                    Text(r.userName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    r.createdAt?.let { Text(DateTimeUtils.formatDateTime(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                                Text("★".repeat(r.rating.coerceIn(0, 5)) + "☆".repeat(5 - r.rating.coerceIn(0, 5)), color = NzColors.Moderate)
                            }
                            if (r.comment.isNotBlank()) Text(r.comment, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

// ---------- 4-step booking wizard with real slots ----------

data class BookingUi(
    val step: Int = 1,
    val types: UiState<List<AppointmentType>> = UiState.Loading,
    val type: AppointmentType? = null,
    val date: LocalDate? = null,
    val slots: UiState<List<String>> = UiState.Empty,
    val slot: String? = null,
    val notes: String = "",
    val submitting: Boolean = false,
    val booked: Boolean = false
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val professionals: ProfessionalRepository,
    private val appointments: AppointmentRepository,
    private val book: BookAppointmentUseCase
) : ViewModel() {
    val professionalId: Long = savedState["id"] ?: 0L
    private val _ui = MutableStateFlow(BookingUi())
    val ui = _ui.asStateFlow()
    val messages = MessageChannel()

    init { loadTypes() }

    fun loadTypes() = viewModelScope.launch { _ui.update { it.copy(types = appointments.getTypes().toUiState()) } }

    fun selectType(type: AppointmentType) = _ui.update { it.copy(type = type, step = 2) }

    fun selectDate(date: LocalDate) {
        _ui.update { it.copy(date = date, slot = null, slots = UiState.Loading, step = 3) }
        loadSlots()
    }

    fun loadSlots() = viewModelScope.launch {
        val date = _ui.value.date ?: return@launch
        _ui.update { it.copy(slots = professionals.getAvailableSlots(professionalId, date).toUiState()) }
    }

    fun selectSlot(slot: String) = _ui.update { it.copy(slot = slot, step = 4) }
    fun back() = _ui.update { it.copy(step = (it.step - 1).coerceAtLeast(1)) }
    fun updateNotes(v: String) = _ui.update { it.copy(notes = v.take(500)) }

    fun confirm() {
        val s = _ui.value
        val type = s.type ?: return
        val date = s.date ?: return
        val slot = s.slot ?: return
        _ui.update { it.copy(submitting = true) }
        viewModelScope.launch {
            when (val r = book(BookingRequest(professionalId, date, slot, type.value, s.notes))) {
                is Outcome.Success -> _ui.update { it.copy(submitting = false, booked = true) }
                is Outcome.Failure -> {
                    messages.send(r.error.message)
                    if (r.error is AppError.Conflict) { _ui.update { it.copy(submitting = false, step = 3, slot = null) }; loadSlots() }
                    else _ui.update { it.copy(submitting = false) }
                }
            }
        }
    }
}

@Composable
fun BookingScreen(onBack: () -> Unit, onBooked: () -> Unit, vm: BookingViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(ui.booked) {
        if (ui.booked) { snackbar.showSnackbar("¡Cita reservada! Queda pendiente de confirmación."); onBooked() }
    }
    Scaffold(
        topBar = { NzTopBar("Reservar sesión · Paso ${ui.step} de 4", onBack = { if (ui.step > 1) vm.back() else onBack() }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LinearProgressIndicator(progress = { ui.step / 4f }, modifier = Modifier.fillMaxWidth())
            when (ui.step) {
                1 -> {
                    Text("¿Qué tipo de sesión necesitas?", style = MaterialTheme.typography.titleMedium)
                    StateContent(ui.types, onRetry = { vm.loadTypes() }) { types ->
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(types, key = { it.value }) { t ->
                                SectionCard(Modifier.clickable { vm.selectType(t) }) {
                                    Text(t.displayName, style = MaterialTheme.typography.titleMedium)
                                    if (t.description.isNotBlank()) Text(t.description)
                                    Text("Duración aprox.: ${t.durationMinutes} min", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    Text("Elige un día", style = MaterialTheme.typography.titleMedium)
                    val today = DateTimeUtils.todayInLima()
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items((1..21).map { today.plusDays(it.toLong()) }) { d ->
                            OutlinedButton(onClick = { vm.selectDate(d) }, modifier = Modifier.fillMaxWidth()) { Text(DateTimeUtils.formatDate(d)) }
                        }
                    }
                }
                3 -> {
                    Text("Horarios libres · ${ui.date?.let(DateTimeUtils::formatDate)} (hora de Lima)", style = MaterialTheme.typography.titleMedium)
                    StateContent(ui.slots, onRetry = { vm.loadSlots() }, emptyMessage = "No hay horarios libres ese día. Prueba otra fecha.",
                        emptyAction = "Elegir otro día" to { vm.back() }) { slots ->
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(slots.chunked(3)) { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { s -> OutlinedButton(onClick = { vm.selectSlot(s) }, modifier = Modifier.weight(1f)) { Text(s) } }
                                    repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
                else -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Confirma tu reserva", style = MaterialTheme.typography.titleMedium)
                    SectionCard {
                        Text("Tipo: ${ui.type?.displayName}")
                        Text("Fecha: ${ui.date?.let(DateTimeUtils::formatDate)}")
                        Text("Hora: ${ui.slot} (Lima)")
                    }
                    NzTextField(ui.notes, vm::updateNotes, "¿Algo que quieras contarle al psicólogo? (opcional)", singleLine = false, minLines = 3)
                    PrimaryButton("Confirmar reserva", vm::confirm, loading = ui.submitting)
                    Text("La cita quedará Pendiente hasta que el psicólogo la confirme.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
