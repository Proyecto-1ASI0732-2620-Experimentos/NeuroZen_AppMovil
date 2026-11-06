package com.example.neurozen_front.presentation.worker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.example.neurozen_front.domain.logic.AppointmentRules
import com.example.neurozen_front.domain.logic.Catalogs
import com.example.neurozen_front.domain.logic.DateTimeUtils
import com.example.neurozen_front.domain.logic.Recommendation
import com.example.neurozen_front.domain.logic.RecommendationAction
import com.example.neurozen_front.domain.logic.RecommendationEngine
import com.example.neurozen_front.domain.logic.StressStatsCalculator
import com.example.neurozen_front.domain.model.Appointment
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.StressLevel
import com.example.neurozen_front.domain.model.StressStats
import com.example.neurozen_front.domain.model.getOrNull
import com.example.neurozen_front.domain.repository.AppointmentRepository
import com.example.neurozen_front.domain.repository.ProfessionalRepository
import com.example.neurozen_front.domain.repository.SessionRepository
import com.example.neurozen_front.domain.repository.StressRepository
import com.example.neurozen_front.domain.repository.WellnessRepository
import com.example.neurozen_front.notifications.ReminderKind
import com.example.neurozen_front.notifications.ReminderScheduler
import com.example.neurozen_front.presentation.components.Pill
import com.example.neurozen_front.presentation.components.SectionTitle
import com.example.neurozen_front.presentation.components.StateContent
import com.example.neurozen_front.presentation.components.StatusPill
import com.example.neurozen_front.presentation.components.UiState
import com.example.neurozen_front.presentation.components.stressColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject

data class HomeData(
    val firstName: String,
    val stats: StressStats,
    val level: StressLevel?,
    val recommendations: List<Recommendation>,
    val nextAppointment: Appointment?,
    val nextAppointmentProfessional: String?,
    val nextBreak: ZonedDateTime?,
    val weeklyHours: Double
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessions: SessionRepository,
    private val stress: StressRepository,
    private val appointments: AppointmentRepository,
    private val professionals: ProfessionalRepository,
    private val wellness: WellnessRepository,
    private val reminders: ReminderScheduler
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val triggersDeferred = async { stress.getTriggers() }
            val assessmentsDeferred = async { stress.getAssessments() }
            val appointmentsDeferred = async { appointments.getMine() }
            val triggers = triggersDeferred.await()
            val assessments = assessmentsDeferred.await()
            if (triggers is Outcome.Failure) { _state.value = UiState.Error(triggers.error.message); return@launch }
            if (assessments is Outcome.Failure) { _state.value = UiState.Error(assessments.error.message); return@launch }
            val triggerList = triggers.getOrNull().orEmpty()
            val assessmentList = assessments.getOrNull().orEmpty()
            val now = Instant.now()
            val stats = StressStatsCalculator.calculate(triggerList, assessmentList, now)

            // Appointments are optional on Home: a failure there should not hide the dashboard.
            val myAppointments = appointmentsDeferred.await().getOrNull().orEmpty()
            reminders.syncAppointments(myAppointments)
            val next = myAppointments.filter { AppointmentRules.isUpcoming(it, now) }.minByOrNull { it.dateTime }
            val professionalName = next?.let { professionals.getById(it.professionalId).getOrNull()?.name }

            val weekStart = LocalDate.now(DateTimeUtils.LIMA).minusDays(6)
            val weeklyHours = wellness.observeWorkload().first().filter { !it.date.isBefore(weekStart) }.sumOf { it.hours }

            val level = assessmentList.firstOrNull()?.level
                ?: triggerList.firstOrNull()?.let { StressLevel.fromTriggerLevel(it.stressLevel) }
            val topCategory = stats.topCategory.takeIf { stats.triggersCount > 0 }

            _state.value = UiState.Success(
                HomeData(
                    firstName = sessions.session.value?.fullName?.substringBefore(' ').orEmpty(),
                    stats = stats,
                    level = level,
                    recommendations = RecommendationEngine.recommend(level, topCategory, weeklyHours, assessmentList.isNotEmpty()).take(3),
                    nextAppointment = next,
                    nextAppointmentProfessional = professionalName,
                    nextBreak = reminders.scheduleNext(ReminderKind.BREAK),
                    weeklyHours = weeklyHours
                )
            )
        }
    }
}

@Composable
fun HomeScreen(onAction: (RecommendationAction) -> Unit, onNavigate: (String) -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    Box(Modifier.fillMaxSize()) {
        StateContent(state, onRetry = vm::load) { data -> HomeContent(data, onAction, onNavigate, onRefresh = vm::load) }
        ExtendedFloatingActionButton(
            onClick = { onNavigate("zenbot") },
            icon = { Icon(Icons.Outlined.Spa, contentDescription = null) },
            text = { Text("ZenBot") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
}

private val HEADER_IMAGES = listOf(
    "https://images.unsplash.com/photo-1506126613408-eca07ce68773?q=80&w=1000",
    "https://images.unsplash.com/photo-1518199266791-5375a83190b7?q=80&w=1000",
    "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?q=80&w=1000",
    "https://images.unsplash.com/photo-1528715471579-d1bcf0ba5e83?q=80&w=1000"
)

private data class MetricItem(val label: String, val value: String, val progress: Float, val detail: String)

@Composable
private fun HomeContent(data: HomeData, onAction: (RecommendationAction) -> Unit, onNavigate: (String) -> Unit, onRefresh: () -> Unit) {
    val headerImage = remember { HEADER_IMAGES.random() }
    val metrics = listOf(
        MetricItem("Estrés actual", "%.0f/10".format(data.stats.current), (data.stats.current / 10).toFloat(), data.level?.label?.let { "Nivel $it" } ?: "Sin registros"),
        MetricItem("Promedio", "%.1f".format(data.stats.average), (data.stats.average / 10).toFloat(), "${data.stats.triggersCount} registros"),
        MetricItem("Último test", data.stats.lastAssessment?.let { "${it.score}/40" } ?: "—", (data.stats.lastAssessment?.score ?: 0) / 40f,
            data.stats.lastAssessment?.completedAt?.let(DateTimeUtils::formatDateTime) ?: "Aún no lo haces"),
        MetricItem("Hora pico", data.stats.peakHours.substringBefore(" "), 0f, "Cuando más te estresas"),
        MetricItem("Horas semana", "%.1f h".format(data.weeklyHours), (data.weeklyHours / RecommendationEngine.WEEKLY_HOURS_LIMIT).toFloat(), "Máximo 48 h")
    )
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Hero header
        item { HeroHeader(data.firstName, headerImage, onRefresh) }

        // 2. Daily quote
        item { DailyQuoteCard(Modifier.padding(horizontal = 20.dp)) }

        // 3. Next appointment
        data.nextAppointment?.let { next ->
            item { NextAppointmentSummary(next, data.nextAppointmentProfessional, Modifier.padding(horizontal = 20.dp)) }
        }

        // 4. Mood selector -> quick access to the right activity
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("¿Cómo te sientes hoy?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MoodItem("Calma", "🌿", MaterialTheme.colorScheme.primaryContainer) { onNavigate("breathing") }
                    MoodItem("Enfoque", "🎯", MaterialTheme.colorScheme.secondaryContainer) { onNavigate("breaks") }
                    MoodItem("Sueño", "🌙", MaterialTheme.colorScheme.tertiaryContainer) { onNavigate("symptoms") }
                    MoodItem("Estrés", "⚡", MaterialTheme.colorScheme.errorContainer) { onNavigate("triggers") }
                }
            }
        }

        // 5. "Tu equilibrio" metrics carousel
        item {
            Column {
                SectionTitle("Tu equilibrio", Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(12.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(metrics) { MetricCard(it, Modifier.width(160.dp)) }
                }
            }
        }

        // 6. Active breaks
        item {
            Card(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("🧘", style = MaterialTheme.typography.titleMedium) }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Pausas activas", style = MaterialTheme.typography.titleMedium)
                        Text(
                            data.nextBreak?.let { "Próxima: ${DateTimeUtils.formatDateTime(it.toInstant())}" } ?: "Aún no programas tus pausas",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { onNavigate("breaks") }) { Text(if (data.nextBreak == null) "Programar" else "Ajustar") }
                }
            }
        }

        // 7. Recommended for you
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("Recomendado para ti")
                Row {
                    TextButton(onClick = { onNavigate("assessment") }) { Text("Test") }
                    TextButton(onClick = { onNavigate("triggers") }) { Text("Check-in") }
                }
            }
        }
        items(data.recommendations, key = { it.title }) { rec ->
            Box(Modifier.padding(horizontal = 20.dp)) { RecommendationCard(rec) { onAction(rec.action) } }
        }
        if (data.stats.triggersCount > 0) {
            item {
                Text(
                    "Tu principal fuente de estrés: ${Catalogs.categoryLabel(data.stats.topCategory)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroHeader(firstName: String, imageUrl: String, onRefresh: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)).background(MaterialTheme.colorScheme.primary)) {
        AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent, Color.Black.copy(alpha = 0.7f)))
            )
        )
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Bottom) {
            Text("Hola, ${firstName.ifBlank { "bienvenido/a" }}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Respira, suelta y recarga", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f))
        }
        FilledIconButton(
            onClick = onRefresh,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
        ) { Icon(Icons.Filled.Refresh, contentDescription = "Actualizar", tint = Color.White) }
    }
}

@Composable
private fun DailyQuoteCard(modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("✨", style = MaterialTheme.typography.titleMedium) }
            }
            Text(
                "“La paz interior comienza cuando decides no permitir que otro controle tus emociones.”",
                style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NextAppointmentSummary(appointment: Appointment, professional: String?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text("Próxima sesión", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("Con ${professional ?: "tu psicólogo"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(DateTimeUtils.formatDateTime(appointment.dateTime), style = MaterialTheme.typography.bodySmall)
            }
            StatusPill(appointment.status)
        }
    }
}

@Composable
private fun MoodItem(name: String, icon: String, containerColor: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(containerColor.copy(alpha = 0.5f))
                .border(2.dp, containerColor, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) { Text(icon, style = MaterialTheme.typography.headlineSmall) }
        Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MetricCard(metric: MetricItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(metric.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(metric.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { metric.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            Text(metric.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

/** Same layout as the original session card ("ProductCard"). */
@Composable
private fun RecommendationCard(rec: Recommendation, onClick: () -> Unit) {
    val (emoji, tag) = when (rec.action) {
        RecommendationAction.BREATHING -> "🌬️" to "Respiración"
        RecommendationAction.BREAKS -> "🧘" to "Pausa activa"
        RecommendationAction.LIBRARY -> "📚" to "Biblioteca"
        RecommendationAction.PROFESSIONALS -> "🩺" to "Profesional"
        RecommendationAction.ASSESSMENT -> "📝" to "Test"
        RecommendationAction.WORKLOAD -> "⏱️" to "Carga laboral"
        RecommendationAction.TRIGGERS -> "📍" to "Registro"
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(emoji, style = MaterialTheme.typography.titleLarge) }
                }
                Text(tag, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text(rec.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(rec.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onClick) { Text("Ver detalle") }
        }
    }
}
