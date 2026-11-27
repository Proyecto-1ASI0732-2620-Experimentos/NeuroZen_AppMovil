package com.example.neurozen_front.presentation.worker

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.neurozen_front.R
import com.example.neurozen_front.domain.logic.BookingDay
import com.example.neurozen_front.domain.logic.CalendarSource
import com.example.neurozen_front.domain.logic.ProfessionalFormat
import com.example.neurozen_front.domain.logic.RatingDisplay
import com.example.neurozen_front.presentation.components.EmptyView
import com.example.neurozen_front.presentation.components.ErrorBanner
import com.example.neurozen_front.presentation.components.InfoBanner
import androidx.compose.material3.TextButton
import com.example.neurozen_front.domain.logic.Catalogs
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
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.Professional
import com.example.neurozen_front.domain.model.Review
import com.example.neurozen_front.domain.model.getOrNull
import com.example.neurozen_front.domain.repository.ProfessionalRepository
import com.example.neurozen_front.presentation.components.MessageChannel
import com.example.neurozen_front.presentation.components.NzTextField
import com.example.neurozen_front.presentation.components.NzTopBar
import com.example.neurozen_front.presentation.components.PrimaryButton
import com.example.neurozen_front.presentation.components.SectionCard
import com.example.neurozen_front.presentation.components.StateContent
import com.example.neurozen_front.presentation.components.UiState
import com.example.neurozen_front.presentation.components.toUiState
import com.example.neurozen_front.presentation.theme.LocalNzDarkTheme
import com.example.neurozen_front.presentation.theme.NzColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
            (specialty == null || it.specialty == specialty) && (query.isBlank() || it.name.contains(query, true) || it.specialty.contains(query, true) || Catalogs.specialtyLabel(it.specialty).contains(query, true))
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { GradientHeader("Centro de Ayuda", "Profesionales verificados por el CPsP, listos para escucharte") }
            item { NzTextField(query, { query = it }, "Buscar por nombre o especialidad", Modifier.padding(horizontal = 16.dp)) }
            if (specialties.isNotEmpty()) item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(specialties) { s -> FilterChip(selected = specialty == s, onClick = { specialty = if (specialty == s) null else s }, label = { Text(Catalogs.specialtyLabel(s)) }) }
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
                    Text(Catalogs.specialtyLabel(p.specialty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    RatingSummary(p.rating, p.reviews)
                }
            }
            if (p.bio.isNotBlank()) Text(p.bio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                p.experienceYears?.let { Tag("$it años exp.") }
                Tag("CPsP verificado")
                Box(Modifier.weight(1f))
                val amount = ProfessionalFormat.priceAmount(p.price)
                Text(
                    amount?.let { stringResource(R.string.price_value, it) } ?: stringResource(R.string.price_to_be_defined),
                    style = if (amount != null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                )
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
                            Text(stringResource(R.string.price_per_session), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val amount = ProfessionalFormat.priceAmount(price)
                            Text(
                                amount?.let { stringResource(R.string.price_value, it) } ?: stringResource(R.string.price_to_be_defined),
                                style = if (amount != null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                            )
                        }
                        PrimaryButton(stringResource(R.string.booking_book_appointment), onBook, Modifier.weight(1.3f))
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
                            title = p.name, subtitle = Catalogs.specialtyLabel(p.specialty),
                            trailing = { Avatar(p.name, 72, p.imageUrl, ring = true) },
                            bottom = {
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text(" Colegiatura verificada por el CPsP", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                }
                                Spacer(Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    val rating = ProfessionalFormat.rating(p.rating, p.reviews)
                                    val ratingA11y = ratingDescription(rating)
                                    HeaderStat(
                                        if (rating is RatingDisplay.Rated) stringResource(R.string.rating_short, rating.text) else stringResource(R.string.stress_index_unknown),
                                        stringResource(R.string.rating_stat_label),
                                        Modifier.weight(1f).clearAndSetSemantics { contentDescription = ratingA11y }
                                    )
                                    HeaderStat(p.reviews.toString(), stringResource(R.string.reviews_stat_label), Modifier.weight(1f))
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
                                val stars = r.rating.coerceIn(0, 5)
                                val starsA11y = stringResource(R.string.review_rating_a11y, stars)
                                Text(
                                    "★".repeat(stars) + "☆".repeat(5 - stars), color = if (LocalNzDarkTheme.current) NzColors.Moderate else NzColors.StarText,
                                    modifier = Modifier.clearAndSetSemantics { contentDescription = starsA11y }
                                )
                            }
                            if (r.comment.isNotBlank()) Text(r.comment, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

// ---------- 4-step booking wizard with real availability (BookingViewModel.kt) ----------

@Composable
fun BookingScreen(onBack: () -> Unit, onBooked: () -> Unit, vm: BookingViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val successMessage = stringResource(R.string.booking_success)
    LaunchedEffect(Unit) { vm.messages.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(ui.booked) {
        if (ui.booked) { snackbar.showSnackbar(successMessage); onBooked() }
    }
    Scaffold(
        topBar = { NzTopBar(stringResource(R.string.booking_title, ui.step), onBack = { if (ui.step > 1) vm.back() else onBack() }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LinearProgressIndicator(progress = { ui.step / 4f }, modifier = Modifier.fillMaxWidth())
            when (ui.step) {
                1 -> BookingTypeStep(ui, vm)
                2 -> BookingDayStep(ui, vm)
                3 -> BookingTimeStep(ui, vm)
                else -> BookingSummaryStep(ui, vm)
            }
        }
    }
}

@Composable
private fun BookingTypeStep(ui: BookingUi, vm: BookingViewModel) {
    Text(stringResource(R.string.booking_type_question), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    StateContent(ui.types, onRetry = { vm.loadTypes() }) { types ->
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(types, key = { it.value }) { t ->
                SectionCard(Modifier.clickable(role = Role.Button) { vm.selectType(t) }) {
                    Text(t.displayName, style = MaterialTheme.typography.titleMedium)
                    if (t.description.isNotBlank()) Text(t.description)
                    Text(stringResource(R.string.booking_type_duration, t.durationMinutes), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun BookingDayStep(ui: BookingUi, vm: BookingViewModel) {
    Text(stringResource(R.string.booking_choose_day), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    TimeZoneNote()
    StateContent(ui.calendar, onRetry = { vm.loadCalendar() }) { calendar ->
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            calendar.nextAvailable?.let { next ->
                item {
                    SectionCard {
                        Text(stringResource(R.string.booking_next_available, DateTimeUtils.formatDate(next)), style = MaterialTheme.typography.titleSmall)
                        OutlinedButton(onClick = { vm.selectDate(next) }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.booking_choose_next))
                        }
                    }
                }
            }
            when (calendar.source) {
                CalendarSource.SCHEDULE -> item { InfoBanner(stringResource(R.string.booking_calendar_schedule_note)) }
                CalendarSource.UNVERIFIED -> item { InfoBanner(stringResource(R.string.booking_calendar_unverified_note)) }
                CalendarSource.BACKEND -> Unit
            }
            if (!calendar.hasEnabledDays && calendar.nextAvailable == null) {
                item { EmptyView(stringResource(R.string.booking_no_days)) }
            } else {
                items(calendar.days.chunked(DAYS_PER_ROW)) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { day -> DayCell(day, ui.date == day.date, Modifier.weight(1f)) { vm.selectDate(day.date) } }
                        repeat(DAYS_PER_ROW - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

private const val DAYS_PER_ROW = 4

/** Day of the calendar: disabled (and announced as such) when it has no free hours. */
@Composable
private fun DayCell(day: BookingDay, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val name = DateTimeUtils.formatDate(day.date)
    val description = stringResource(if (day.enabled) R.string.booking_day_available_a11y else R.string.booking_day_unavailable_a11y, name)
    val colors = if (selected) androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    else androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
    OutlinedButton(
        onClick = onClick, enabled = day.enabled, colors = colors,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        modifier = modifier.heightIn(min = 64.dp).semantics { contentDescription = description }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(DateTimeUtils.dayShort(day.date.dayOfWeek), style = MaterialTheme.typography.labelMedium)
            Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(DateTimeUtils.monthShort(day.date), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TimeZoneNote() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Text(stringResource(R.string.booking_timezone), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BookingTimeStep(ui: BookingUi, vm: BookingViewModel) {
    Text(
        stringResource(R.string.booking_slots_title, ui.date?.let(DateTimeUtils::formatDate).orEmpty()),
        style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() }
    )
    TimeZoneNote()
    ui.notice?.let { ErrorBanner(stringResource(it)) }
    StateContent(
        ui.slots, onRetry = { vm.loadSlots() }, emptyMessage = stringResource(R.string.booking_no_slots),
        emptyAction = stringResource(R.string.booking_choose_other_day) to { vm.changeDate() }
    ) { slots ->
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(slots.chunked(3)) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { s -> OutlinedButton(onClick = { vm.selectSlot(s) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(s) } }
                    repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun BookingSummaryStep(ui: BookingUi, vm: BookingViewModel) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.booking_confirm_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        SectionCard {
            SummaryRow(stringResource(R.string.booking_summary_type, ui.type?.displayName.orEmpty()), R.string.booking_change_type_a11y, vm::changeType)
            SummaryRow(stringResource(R.string.booking_summary_date, ui.date?.let(DateTimeUtils::formatDate).orEmpty()), R.string.booking_change_date_a11y, vm::changeDate)
            SummaryRow(stringResource(R.string.booking_summary_time, ui.slot.orEmpty()), R.string.booking_change_time_a11y, vm::changeTime)
        }
        NzTextField(ui.notes, vm::updateNotes, stringResource(R.string.booking_notes), singleLine = false, minLines = 3)
        PrimaryButton(stringResource(R.string.booking_confirm), vm::confirm, loading = ui.submitting)
        Text(stringResource(R.string.booking_pending_note), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SummaryRow(text: String, @StringRes changeDescription: Int, onChange: () -> Unit) {
    val a11y = stringResource(changeDescription)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, modifier = Modifier.weight(1f))
        TextButton(onClick = onChange, modifier = Modifier.semantics { contentDescription = a11y }) { Text(stringResource(R.string.booking_change)) }
    }
}

// ---------- Rating and price without filler values (B1, M1) ----------

@Composable
private fun ratingDescription(rating: RatingDisplay): String = when (rating) {
    RatingDisplay.None -> stringResource(R.string.rating_none)
    is RatingDisplay.Rated -> stringResource(
        R.string.rating_a11y, rating.text, pluralStringResource(R.plurals.reviews_count, rating.reviews, rating.reviews)
    )
}

/** «★ 4,5 (12 reseñas)» read as one sentence; «Sin valoraciones aún» when there are no reviews. */
@Composable
private fun RatingSummary(rating: Double, reviews: Int) {
    val display = ProfessionalFormat.rating(rating, reviews)
    val description = ratingDescription(display)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clearAndSetSemantics { contentDescription = description }) {
        when (display) {
            RatingDisplay.None -> Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            is RatingDisplay.Rated -> {
                Icon(Icons.Filled.Star, contentDescription = null, tint = NzColors.Moderate, modifier = Modifier.size(16.dp))
                Text(" ${display.text}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    "  (" + pluralStringResource(R.plurals.reviews_count, display.reviews, display.reviews) + ")",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
