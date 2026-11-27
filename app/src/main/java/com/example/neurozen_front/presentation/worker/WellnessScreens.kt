package com.example.neurozen_front.presentation.worker

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.neurozen_front.R
import com.example.neurozen_front.data.feedback.AndroidFeedbackOutput
import com.example.neurozen_front.data.settings.BreathingFeedbackSettingsStore
import com.example.neurozen_front.domain.logic.BreathPhaseKind
import com.example.neurozen_front.domain.logic.BreathingFeedbackController
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.WorkHistory
import com.example.neurozen_front.presentation.components.Accent
import com.example.neurozen_front.presentation.components.FeatureTile
import com.example.neurozen_front.presentation.components.SectionHeader
import com.example.neurozen_front.presentation.components.GradientHeader
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neurozen_front.domain.logic.Catalogs
import com.example.neurozen_front.domain.logic.DateTimeUtils
import com.example.neurozen_front.domain.logic.RecommendationEngine
import com.example.neurozen_front.domain.logic.Validators
import com.example.neurozen_front.domain.model.ChallengeProgress
import com.example.neurozen_front.domain.repository.WellnessRepository
import com.example.neurozen_front.notifications.NotificationHelper
import com.example.neurozen_front.notifications.ReminderConfig
import com.example.neurozen_front.notifications.ReminderKind
import com.example.neurozen_front.notifications.ReminderScheduler
import com.example.neurozen_front.presentation.components.ConfirmDialog
import com.example.neurozen_front.presentation.components.InfoBanner
import com.example.neurozen_front.presentation.components.MessageChannel
import com.example.neurozen_front.presentation.components.NzTextField
import com.example.neurozen_front.presentation.components.NzTopBar
import com.example.neurozen_front.presentation.components.PrimaryButton
import com.example.neurozen_front.presentation.components.SectionCard
import com.example.neurozen_front.presentation.theme.NzColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject

private data class HubEntry(val route: String, val title: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val tint: androidx.compose.ui.graphics.Color)

@Composable
fun WellnessHubScreen(onNavigate: (String) -> Unit) {
    val entries = listOf(
        HubEntry("breathing", "Respiración guiada", "3 ejercicios con animación", Icons.Outlined.Air, Accent.Teal),
        HubEntry("breaks", "Pausas y ergonomía", "Programa tus recordatorios", Icons.Outlined.SelfImprovement, Accent.Emerald),
        HubEntry("triggers", "Registro de estrés", "Anota qué te estresa", Icons.Outlined.EditNote, Accent.Coral),
        HubEntry("symptoms", "Síntomas físicos", "Sueño, tensión, cabeza…", Icons.Outlined.MonitorHeart, Accent.Purple),
        HubEntry("assessment", "Autoevaluación", "10 preguntas · 3 min", Icons.Outlined.Quiz, Accent.Blue),
        HubEntry("workload", "Carga laboral", "Horas por semana", Icons.Outlined.WorkHistory, Accent.Amber),
        HubEntry("challenges", "Desafíos", "Hábitos antiestrés", Icons.Outlined.EmojiEvents, Accent.Deep),
        HubEntry("library", "Biblioteca", "Artículos, audios y videos", Icons.AutoMirrored.Outlined.MenuBook, Accent.Green)
    )
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { GradientHeader("Bienestar", "Herramientas para tu equilibrio diario") }
        item { SectionHeader("Explora", Modifier.padding(horizontal = 20.dp)) }
        items(entries.chunked(2)) { row ->
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { e -> FeatureTile(e.icon, e.title, e.subtitle, e.tint, Modifier.weight(1f)) { onNavigate(e.route) } }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

// ---------- Guided breathing (US08), stored locally in Room ----------

@HiltViewModel
class BreathingViewModel @Inject constructor(
    private val wellness: WellnessRepository,
    private val feedbackSettings: BreathingFeedbackSettingsStore
) : ViewModel() {
    val sessions = wellness.observeBreathingSessions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val feedback = feedbackSettings.preferences
    fun complete(exerciseId: String, seconds: Int) = viewModelScope.launch { wellness.addBreathingSession(exerciseId, seconds) }
    fun setVibration(enabled: Boolean) = feedbackSettings.setVibration(enabled)
    fun setSound(enabled: Boolean) = feedbackSettings.setSound(enabled)
}

@StringRes
private fun phaseLabel(kind: BreathPhaseKind): Int = when (kind) {
    BreathPhaseKind.INHALE -> R.string.breathing_phase_inhale
    BreathPhaseKind.HOLD -> R.string.breathing_phase_hold
    BreathPhaseKind.EXHALE -> R.string.breathing_phase_exhale
}

@Composable
fun BreathingScreen(onBack: () -> Unit, vm: BreathingViewModel = hiltViewModel()) {
    val history by vm.sessions.collectAsState()
    val prefs by vm.feedback.collectAsState()
    var selected by rememberSaveable { mutableStateOf(Catalogs.BREATHING_EXERCISES.first().id) }
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var phaseIndex by remember { mutableIntStateOf(-1) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var cycle by remember { mutableIntStateOf(0) }
    val exercise = Catalogs.BREATHING_EXERCISES.first { it.id == selected }
    val phase = exercise.phases.getOrNull(phaseIndex)

    // Haptic/sound cues: pure policy + Android output. Released on stop, pause, finish and when leaving the screen.
    val context = LocalContext.current
    val currentPrefs by rememberUpdatedState(prefs)
    val feedback = remember { BreathingFeedbackController(AndroidFeedbackOutput(context)) { currentPrefs } }
    DisposableEffect(feedback) { onDispose { feedback.stop() } }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { running = false; phaseIndex = -1; feedback.stop() }

    LaunchedEffect(running, selected) {
        if (!running) return@LaunchedEffect
        finished = false
        val total = exercise.phases.sumOf { it.seconds } * exercise.cycles
        try {
            for (c in 1..exercise.cycles) {
                cycle = c
                exercise.phases.forEachIndexed { i, p ->
                    phaseIndex = i
                    feedback.onPhase(p.kind)
                    for (s in p.seconds downTo 1) { secondsLeft = s; delay(1_000) }
                }
            }
            vm.complete(exercise.id, total)
            finished = true
            running = false
            phaseIndex = -1
        } finally {
            // Also runs when the effect is cancelled (Detener, pause or leaving the screen): no cue keeps playing.
            feedback.stop()
        }
    }

    val previous = exercise.phases.getOrNull(phaseIndex - 1)?.kind
    val target = when {
        !running || phase == null -> 0.6f
        phase.kind == BreathPhaseKind.INHALE -> 1f
        phase.kind == BreathPhaseKind.EXHALE -> 0.5f
        else -> if (previous == BreathPhaseKind.INHALE) 1f else 0.5f
    }
    val scale by animateFloatAsState(target, tween(durationMillis = (phase?.seconds ?: 1) * 1000), label = "breath")
    val label = when {
        running && phase != null -> stringResource(phaseLabel(phase.kind))
        finished -> stringResource(R.string.breathing_done)
        else -> stringResource(R.string.breathing_ready)
    }
    val a11yLabel = if (running && phase != null) pluralStringResource(R.plurals.breathing_phase_a11y, secondsLeft, label, secondsLeft) else label

    Scaffold(topBar = { NzTopBar(stringResource(R.string.breathing_title), onBack) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Catalogs.BREATHING_EXERCISES) { e ->
                    FilterChip(selected = selected == e.id, enabled = !running, onClick = { selected = e.id; finished = false }, label = { Text(e.name) })
                }
            }
            Text(exercise.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(220.dp).scale(scale).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f), CircleShape))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clearAndSetSemantics { liveRegion = LiveRegionMode.Polite; contentDescription = a11yLabel }
                ) {
                    Text(label, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    if (running) {
                        Text(stringResource(R.string.breathing_progress, secondsLeft, cycle, exercise.cycles), color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            PrimaryButton(
                stringResource(if (running) R.string.breathing_stop else R.string.breathing_start),
                onClick = { running = !running; if (!running) { phaseIndex = -1; feedback.stop() } }
            )
            SectionCard {
                Text(stringResource(R.string.breathing_feedback_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                FeedbackSwitch(Icons.Outlined.Vibration, R.string.breathing_vibration, R.string.breathing_vibration_detail, prefs.vibration, vm::setVibration)
                FeedbackSwitch(Icons.AutoMirrored.Outlined.VolumeUp, R.string.breathing_sound, R.string.breathing_sound_detail, prefs.sound, vm::setSound)
                Text(stringResource(R.string.breathing_feedback_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(stringResource(R.string.breathing_history, history.size), style = MaterialTheme.typography.titleMedium)
            history.take(10).forEach { s ->
                val name = Catalogs.BREATHING_EXERCISES.firstOrNull { it.id == s.exerciseId }?.name ?: s.exerciseId
                Text(
                    stringResource(R.string.breathing_history_item, name, s.durationSeconds / 60, s.durationSeconds % 60, DateTimeUtils.formatDateTime(s.completedAt)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** Whole row is the touch target (>= 48 dp); TalkBack reads «Vibración, interruptor, activado». */
@Composable
private fun FeedbackSwitch(icon: ImageVector, @StringRes title: Int, @StringRes detail: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(detail), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

// ---------- Active breaks + ergonomics reminders (US09, US20) ----------

@HiltViewModel
class RemindersViewModel @Inject constructor(private val scheduler: ReminderScheduler) : ViewModel() {
    private val _breaks = MutableStateFlow(scheduler.store.get(ReminderKind.BREAK))
    val breaks = _breaks.asStateFlow()
    private val _ergo = MutableStateFlow(scheduler.store.get(ReminderKind.ERGONOMICS))
    val ergonomics = _ergo.asStateFlow()
    val messages = MessageChannel()

    fun update(kind: ReminderKind, transform: (ReminderConfig) -> ReminderConfig) {
        if (kind == ReminderKind.BREAK) _breaks.value = transform(_breaks.value) else _ergo.value = transform(_ergo.value)
    }

    fun save(kind: ReminderKind): ZonedDateTime? {
        val config = if (kind == ReminderKind.BREAK) _breaks.value else _ergo.value
        if (config.enabled && !config.end.isAfter(config.start)) { messages.send("La hora de fin debe ser posterior al inicio"); return null }
        scheduler.store.save(kind, config)
        val next = scheduler.scheduleNext(kind)
        messages.send(
            if (!config.enabled) "Recordatorios desactivados"
            else next?.let { "Próximo recordatorio: ${DateTimeUtils.formatDateTime(it.toInstant())}" } ?: "Elige al menos un día"
        )
        return next
    }
}

@Composable
fun RemindersScreen(onBack: () -> Unit, vm: RemindersViewModel = hiltViewModel()) {
    val breaks by vm.breaks.collectAsState()
    val ergo by vm.ergonomics.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) { vm.messages.messages.collect { snackbar.showSnackbar(it) } }
    Scaffold(topBar = { NzTopBar("Pausas y ergonomía", onBack) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (!NotificationHelper.canNotify(context)) {
                InfoBanner("Activa las notificaciones de NeuroZen en los ajustes del teléfono para recibir los recordatorios.", NzColors.Moderate)
            }
            ReminderEditor("Pausas activas", "Te avisaremos para estirarte y respirar.", breaks, { t -> vm.update(ReminderKind.BREAK, t) }, { vm.save(ReminderKind.BREAK) })
            ReminderEditor("Ergonomía", "Recordatorios para revisar tu postura y tu pantalla.", ergo, { t -> vm.update(ReminderKind.ERGONOMICS, t) }, { vm.save(ReminderKind.ERGONOMICS) })
            Text(
                "Los recordatorios se programan en tu teléfono y pueden llegar con unos minutos de diferencia para ahorrar batería.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReminderEditor(
    title: String,
    subtitle: String,
    config: ReminderConfig,
    onChange: ((ReminderConfig) -> ReminderConfig) -> Unit,
    onSave: () -> Unit
) {
    var start by remember(config.start) { mutableStateOf(config.start.toString()) }
    var end by remember(config.end) { mutableStateOf(config.end.toString()) }
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = config.enabled, onCheckedChange = { v -> onChange { it.copy(enabled = v) } })
        }
        if (config.enabled) {
            Text("Días", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(DayOfWeek.entries.toList()) { day ->
                    FilterChip(
                        selected = day in config.days,
                        onClick = { onChange { c -> c.copy(days = if (day in c.days) c.days - day else c.days + day) } },
                        label = { Text(DateTimeUtils.dayShort(day)) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NzTextField(start, { v -> start = v; if (Validators.isValidHour(v)) onChange { it.copy(start = LocalTime.parse(v)) } }, "Desde (HH:mm)", Modifier.weight(1f),
                    error = if (Validators.isValidHour(start)) null else "HH:mm")
                NzTextField(end, { v -> end = v; if (Validators.isValidHour(v)) onChange { it.copy(end = LocalTime.parse(v)) } }, "Hasta (HH:mm)", Modifier.weight(1f),
                    error = if (Validators.isValidHour(end)) null else "HH:mm")
            }
            Text("Cada", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf(30, 45, 60, 90, 120)) { minutes ->
                    FilterChip(selected = config.everyMinutes == minutes, onClick = { onChange { it.copy(everyMinutes = minutes) } }, label = { Text("$minutes min") })
                }
            }
        }
        PrimaryButton("Guardar", onSave)
    }
}

// ---------- Workload (US19) local ----------

@HiltViewModel
class WorkloadViewModel @Inject constructor(private val wellness: WellnessRepository) : ViewModel() {
    val entries = wellness.observeWorkload().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val weeklyHours = wellness.observeWorkload()
        .map { list -> val from = LocalDate.now(DateTimeUtils.LIMA).minusDays(6); list.filter { !it.date.isBefore(from) }.sumOf { it.hours } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val messages = MessageChannel()

    fun add(daysAgo: Int, hoursText: String, note: String) {
        val hours = hoursText.replace(',', '.').toDoubleOrNull()
        if (hours == null || hours <= 0 || hours > 24) { messages.send("Ingresa horas entre 0 y 24"); return }
        viewModelScope.launch {
            wellness.addWorkload(LocalDate.now(DateTimeUtils.LIMA).minusDays(daysAgo.toLong()), hours, note)
            messages.send("Horas registradas")
        }
    }

    fun delete(id: Long) = viewModelScope.launch { wellness.deleteWorkload(id) }
}

@Composable
fun WorkloadScreen(onBack: () -> Unit, onReminders: () -> Unit, vm: WorkloadViewModel = hiltViewModel()) {
    val entries by vm.entries.collectAsState()
    val weekly by vm.weeklyHours.collectAsState()
    var hours by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var daysAgo by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.messages.collect { snackbar.showSnackbar(it) } }
    Scaffold(topBar = { NzTopBar("Carga laboral", onBack) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionCard {
                    Text("Últimos 7 días", style = MaterialTheme.typography.titleMedium)
                    Text("${"%.1f".format(weekly)} h de ${RecommendationEngine.WEEKLY_HOURS_LIMIT.toInt()} h", style = MaterialTheme.typography.headlineSmall)
                    LinearProgressIndicator(
                        progress = { (weekly / RecommendationEngine.WEEKLY_HOURS_LIMIT).toFloat().coerceIn(0f, 1f) },
                        color = if (weekly > RecommendationEngine.WEEKLY_HOURS_LIMIT) NzColors.High else NzColors.Low,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (weekly > RecommendationEngine.WEEKLY_HOURS_LIMIT) {
                        Text("Superas la jornada máxima de 48 h semanales. Intenta desconectarte y programa pausas.", color = NzColors.High)
                    }
                    TextButton(onClick = onReminders) { Text("Configurar recordatorios de ergonomía") }
                }
            }
            item {
                SectionCard {
                    Text("Registrar horas", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items((0..6).toList()) { d ->
                            val date = LocalDate.now(DateTimeUtils.LIMA).minusDays(d.toLong())
                            FilterChip(selected = daysAgo == d, onClick = { daysAgo = d }, label = { Text(if (d == 0) "Hoy" else "${DateTimeUtils.dayShort(date.dayOfWeek)} ${date.dayOfMonth}") })
                        }
                    }
                    NzTextField(hours, { hours = it }, "Horas trabajadas", keyboardType = KeyboardType.Decimal)
                    NzTextField(note, { note = it.take(120) }, "Nota (opcional)")
                    PrimaryButton("Guardar", onClick = { vm.add(daysAgo, hours, note); hours = ""; note = "" })
                }
            }
            items(entries, key = { it.id }) { e ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(DateTimeUtils.formatDate(e.date), fontWeight = FontWeight.SemiBold)
                            Text("${e.hours} h" + if (e.note.isNotBlank()) " · ${e.note}" else "")
                        }
                        IconButton(onClick = { vm.delete(e.id) }) { Icon(Icons.Outlined.Delete, contentDescription = "Eliminar") }
                    }
                }
            }
        }
    }
}

// ---------- Personal anti-stress challenges (US18) local ----------

@HiltViewModel
class ChallengesViewModel @Inject constructor(private val wellness: WellnessRepository) : ViewModel() {
    val progress = wellness.observeChallenges().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun completeToday(id: String) = viewModelScope.launch { wellness.completeChallengeDay(id, LocalDate.now(DateTimeUtils.LIMA)) }
    fun reset(id: String) = viewModelScope.launch { wellness.resetChallenge(id) }
}

@Composable
fun ChallengesScreen(onBack: () -> Unit, vm: ChallengesViewModel = hiltViewModel()) {
    val progress by vm.progress.collectAsState()
    var toReset by remember { mutableStateOf<String?>(null) }
    val today = LocalDate.now(DateTimeUtils.LIMA)
    Scaffold(topBar = { NzTopBar("Desafíos antiestrés", onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(Catalogs.CHALLENGES) { c ->
                val p: ChallengeProgress? = progress.firstOrNull { it.challengeId == c.id }
                val done = p?.completedDays ?: 0
                val finished = done >= c.days
                SectionCard {
                    Text(c.title, style = MaterialTheme.typography.titleMedium)
                    Text(c.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(progress = { (done.toFloat() / c.days).coerceAtMost(1f) }, modifier = Modifier.fillMaxWidth())
                    Text(if (finished) "¡Completado! 🎉" else "$done de ${c.days} días")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.completeToday(c.id) }, enabled = !finished && p?.lastCompletedDate != today) {
                            Text(if (p?.lastCompletedDate == today) "Hecho hoy ✓" else "Marcar hoy")
                        }
                        if (done > 0) TextButton(onClick = { toReset = c.id }) { Text("Reiniciar") }
                    }
                }
            }
        }
    }
    toReset?.let { id -> ConfirmDialog("Reiniciar desafío", "Perderás el progreso de este desafío.", "Reiniciar", { vm.reset(id) }, { toReset = null }) }
}
