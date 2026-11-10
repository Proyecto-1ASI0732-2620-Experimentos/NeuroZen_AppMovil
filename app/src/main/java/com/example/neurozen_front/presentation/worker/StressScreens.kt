package com.example.neurozen_front.presentation.worker

import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Work
import com.example.neurozen_front.presentation.components.IconBadge
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neurozen_front.domain.logic.AssessmentScoring
import com.example.neurozen_front.domain.logic.Catalogs
import com.example.neurozen_front.domain.logic.DateTimeUtils
import com.example.neurozen_front.domain.logic.RecommendationAction
import com.example.neurozen_front.domain.logic.RecommendationEngine
import com.example.neurozen_front.domain.model.Assessment
import com.example.neurozen_front.domain.model.HealthMetric
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.StressLevel
import com.example.neurozen_front.domain.model.StressTrigger
import com.example.neurozen_front.domain.repository.StressRepository
import com.example.neurozen_front.presentation.components.ConfirmDialog
import com.example.neurozen_front.presentation.components.MessageChannel
import com.example.neurozen_front.presentation.components.NzTextField
import com.example.neurozen_front.presentation.components.NzTopBar
import com.example.neurozen_front.presentation.components.Pill
import com.example.neurozen_front.presentation.components.PrimaryButton
import com.example.neurozen_front.presentation.components.SectionCard
import com.example.neurozen_front.presentation.components.StateContent
import com.example.neurozen_front.presentation.components.UiState
import com.example.neurozen_front.presentation.components.stressColor
import com.example.neurozen_front.presentation.components.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

// ---------- Stress records (US12) ----------

data class TriggerForm(val category: String = "work", val level: Int = 5, val description: String = "", val saving: Boolean = false)

@HiltViewModel
class TriggersViewModel @Inject constructor(private val stress: StressRepository) : ViewModel() {
    private val _list = MutableStateFlow<UiState<List<StressTrigger>>>(UiState.Loading)
    val list = _list.asStateFlow()
    private val _form = MutableStateFlow(TriggerForm())
    val form = _form.asStateFlow()
    val messages = MessageChannel()

    init { load() }

    fun load() = viewModelScope.launch {
        _list.value = UiState.Loading
        _list.value = stress.getTriggers().toUiState()
    }

    fun updateForm(transform: (TriggerForm) -> TriggerForm) = _form.update(transform)

    /** Two taps: pick category + level and save. Description is optional. */
    fun save() {
        val f = _form.value
        _form.update { it.copy(saving = true) }
        viewModelScope.launch {
            val description = f.description.ifBlank { Catalogs.categoryLabel(f.category) }
            when (val r = stress.addTrigger(f.category, f.level, description, Instant.now())) {
                is Outcome.Success -> { _form.value = TriggerForm(); messages.send("Registro guardado"); load() }
                is Outcome.Failure -> { _form.update { it.copy(saving = false) }; messages.send(r.error.message) }
            }
        }
    }

    fun delete(id: Long) = viewModelScope.launch {
        when (val r = stress.deleteTrigger(id)) {
            is Outcome.Success -> { messages.send("Registro eliminado"); load() }
            is Outcome.Failure -> messages.send(r.error.message)
        }
    }
}

@Composable
fun TriggersScreen(onBack: () -> Unit, vm: TriggersViewModel = hiltViewModel()) {
    val list by vm.list.collectAsState()
    val form by vm.form.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var toDelete by remember { mutableStateOf<StressTrigger?>(null) }
    LaunchedEffect(Unit) { vm.messages.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(topBar = { NzTopBar("Registro de estrés", onBack) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionCard {
                    Text("¿Qué te estresó?", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Catalogs.TRIGGER_CATEGORIES) { c ->
                            FilterChip(selected = form.category == c.key, onClick = { vm.updateForm { it.copy(category = c.key) } }, label = { Text(c.label) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Nivel: ${form.level}/10", modifier = Modifier.weight(1f))
                        Pill(StressLevel.fromTriggerLevel(form.level).label, stressColor(StressLevel.fromTriggerLevel(form.level)))
                    }
                    Slider(
                        value = form.level.toFloat(), onValueChange = { v -> vm.updateForm { it.copy(level = v.toInt()) } },
                        valueRange = 1f..10f, steps = 8
                    )
                    NzTextField(form.description, { v -> vm.updateForm { it.copy(description = v.take(500)) } }, "Descripción (opcional)", singleLine = false, minLines = 2)
                    PrimaryButton("Guardar registro", vm::save, loading = form.saving)
                }
            }
            item { Text("Historial", style = MaterialTheme.typography.titleMedium) }
            item {
                if (list !is UiState.Success) StateContent(list, onRetry = { vm.load() }, emptyMessage = "Aún no tienes registros. ¡Empieza arriba!") { }
            }
            (list as? UiState.Success)?.data?.let { triggers ->
                items(triggers, key = { it.id }) { t ->
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconBadge(categoryIcon(t.category), stressColor(StressLevel.fromTriggerLevel(t.stressLevel)))
                            Column(Modifier.weight(1f)) {
                                Text(Catalogs.categoryLabel(t.category), fontWeight = FontWeight.SemiBold)
                                Text(DateTimeUtils.formatDateTime(t.triggeredAt), style = MaterialTheme.typography.bodySmall)
                            }
                            Pill("${t.stressLevel}/10", stressColor(StressLevel.fromTriggerLevel(t.stressLevel)))
                            IconButton(onClick = { toDelete = t }) { Icon(Icons.Outlined.Delete, contentDescription = "Eliminar registro") }
                        }
                        if (t.description.isNotBlank()) Text(t.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
    toDelete?.let { t ->
        ConfirmDialog("Eliminar registro", "¿Seguro que quieres eliminar este registro?", "Eliminar", { vm.delete(t.id) }, { toDelete = null }, destructive = true)
    }
}

// ---------- Self-assessment (US04) ----------

data class AssessmentUi(
    val answers: List<Int?> = List(AssessmentScoring.QUESTION_COUNT) { null },
    val current: Int = 0,
    val submitting: Boolean = false,
    val result: Assessment? = null,
    val error: String? = null
)

@HiltViewModel
class AssessmentViewModel @Inject constructor(private val stress: StressRepository) : ViewModel() {
    private val _ui = MutableStateFlow(AssessmentUi())
    val ui = _ui.asStateFlow()
    private val _history = MutableStateFlow<UiState<List<Assessment>>>(UiState.Loading)
    val history = _history.asStateFlow()

    init { loadHistory() }

    fun loadHistory() = viewModelScope.launch { _history.value = stress.getAssessments().toUiState() }

    fun answer(value: Int) = _ui.update { s ->
        val updated = s.answers.toMutableList().also { it[s.current] = value }
        s.copy(answers = updated, current = (s.current + 1).coerceAtMost(AssessmentScoring.QUESTION_COUNT - 1), error = null)
    }

    fun goTo(index: Int) = _ui.update { it.copy(current = index.coerceIn(0, AssessmentScoring.QUESTION_COUNT - 1)) }

    fun submit() {
        val s = _ui.value
        if (!AssessmentScoring.isComplete(s.answers)) {
            _ui.update { it.copy(error = "Responde las 10 preguntas") }
            return
        }
        _ui.update { it.copy(submitting = true) }
        viewModelScope.launch {
            when (val r = stress.submitAssessment(s.answers.map { it!! })) {
                is Outcome.Success -> { _ui.update { it.copy(submitting = false, result = r.data) }; loadHistory() }
                is Outcome.Failure -> _ui.update { it.copy(submitting = false, error = r.error.message) }
            }
        }
    }

    fun restart() { _ui.value = AssessmentUi() }
}

@Composable
fun AssessmentScreen(onBack: () -> Unit, onAction: (RecommendationAction) -> Unit, vm: AssessmentViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val history by vm.history.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(topBar = { NzTopBar("Test de autoevaluación", onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Test") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Historial") })
            }
            if (tab == 0) {
                val result = ui.result
                if (result != null) AssessmentResult(result, onAction, vm::restart) else AssessmentQuestions(ui, vm)
            } else {
                StateContent(history, onRetry = { vm.loadHistory() }, emptyMessage = "Todavía no has hecho el test.") { list ->
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(list, key = { it.id }) { a ->
                            SectionCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(DateTimeUtils.formatDateTime(a.completedAt), modifier = Modifier.weight(1f))
                                    Pill("${a.score}/40 · ${a.level.label}", stressColor(a.level))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssessmentQuestions(ui: AssessmentUi, vm: AssessmentViewModel) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val answered = ui.answers.count { it != null }
        LinearProgressIndicator(progress = { answered / 10f }, modifier = Modifier.fillMaxWidth())
        Text("Pregunta ${ui.current + 1} de 10", style = MaterialTheme.typography.labelLarge)
        Text(Catalogs.ASSESSMENT_QUESTIONS[ui.current], style = MaterialTheme.typography.titleMedium)
        Catalogs.ANSWER_OPTIONS.forEachIndexed { index, label ->
            Row(
                Modifier.fillMaxWidth().selectable(selected = ui.answers[ui.current] == index, onClick = { vm.answer(index) }, role = Role.RadioButton).padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = ui.answers[ui.current] == index, onClick = null)
                Text(label, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.goTo(ui.current - 1) }, enabled = ui.current > 0) { Text("Anterior") }
            OutlinedButton(onClick = { vm.goTo(ui.current + 1) }, enabled = ui.current < 9) { Text("Siguiente") }
        }
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (answered == 10) PrimaryButton("Ver mi resultado", vm::submit, loading = ui.submitting)
        Text(
            "Este test es orientativo y no constituye un diagnóstico.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AssessmentResult(result: Assessment, onAction: (RecommendationAction) -> Unit, onRestart: () -> Unit) {
    val recs = RecommendationEngine.recommend(result.level, null, null, hasAssessment = true)
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard {
                Text("Tu resultado", style = MaterialTheme.typography.titleMedium)
                Text("${result.score} / 40", style = MaterialTheme.typography.headlineMedium, color = stressColor(result.level))
                Pill("Estrés ${result.level.label.lowercase()}", stressColor(result.level))
                Text(
                    when (result.level) {
                        StressLevel.LOW -> "Manejas bien la presión. Sigue cuidando tus pausas y tu descanso."
                        StressLevel.MODERATE -> "Hay señales de estrés. Te ayudará practicar respiración y pausas activas."
                        StressLevel.HIGH -> "Tu nivel de estrés es alto. Te recomendamos conversar con un psicólogo."
                    }
                )
            }
        }
        itemsIndexed(recs) { _, rec ->
            SectionCard {
                Text(rec.title, fontWeight = FontWeight.SemiBold)
                Text(rec.description, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { onAction(rec.action) }) { Text("Ir") }
            }
        }
        item { OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Hacer el test otra vez") } }
    }
}

// ---------- Physical symptoms (US06) via /users/{id}/health_metrics ----------

data class SymptomsForm(
    val selected: Set<String> = emptySet(),
    val stressLevel: Int = 5,
    val sleepHours: String = "",
    val heartRate: String = "",
    val notes: String = "",
    val saving: Boolean = false
)

@HiltViewModel
class SymptomsViewModel @Inject constructor(private val stress: StressRepository) : ViewModel() {
    private val _form = MutableStateFlow(SymptomsForm())
    val form = _form.asStateFlow()
    private val _history = MutableStateFlow<UiState<List<HealthMetric>>>(UiState.Loading)
    val history = _history.asStateFlow()
    val messages = MessageChannel()

    init { load() }

    fun load() = viewModelScope.launch { _history.value = stress.getHealthMetrics().toUiState() }

    fun update(transform: (SymptomsForm) -> SymptomsForm) = _form.update(transform)

    fun toggle(symptom: String) = _form.update { f -> f.copy(selected = if (symptom in f.selected) f.selected - symptom else f.selected + symptom) }

    fun save() {
        val f = _form.value
        val sleep = f.sleepHours.replace(',', '.').toDoubleOrNull()
        val heart = f.heartRate.toIntOrNull()
        if (f.sleepHours.isNotBlank() && (sleep == null || sleep !in 0.0..24.0)) { messages.send("Horas de sueño inválidas"); return }
        if (f.heartRate.isNotBlank() && (heart == null || heart !in 30..220)) { messages.send("Frecuencia cardíaca inválida"); return }
        if (f.selected.isEmpty() && f.notes.isBlank()) { messages.send("Elige al menos un síntoma o escribe una nota"); return }
        val notes = buildString {
            if (f.selected.isNotEmpty()) append("Síntomas: ").append(f.selected.joinToString(", "))
            if (f.notes.isNotBlank()) { if (isNotEmpty()) append(". "); append(f.notes.trim()) }
        }
        _form.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val r = stress.addHealthMetric(HealthMetric(null, f.stressLevel, heart, sleep, notes, null))) {
                is Outcome.Success -> { _form.value = SymptomsForm(); messages.send("Síntomas registrados"); load() }
                is Outcome.Failure -> { _form.update { it.copy(saving = false) }; messages.send(r.error.message) }
            }
        }
    }
}

@Composable
fun SymptomsScreen(onBack: () -> Unit, vm: SymptomsViewModel = hiltViewModel()) {
    val form by vm.form.collectAsState()
    val history by vm.history.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.messages.collect { snackbar.showSnackbar(it) } }
    Scaffold(topBar = { NzTopBar("Síntomas físicos", onBack) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionCard {
                    Text("¿Qué sientes hoy?", style = MaterialTheme.typography.titleMedium)
                    Catalogs.SYMPTOMS.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { s -> FilterChip(selected = s in form.selected, onClick = { vm.toggle(s) }, label = { Text(s) }) }
                        }
                    }
                    Text("Estrés percibido: ${form.stressLevel}/10")
                    Slider(form.stressLevel.toFloat(), { v -> vm.update { it.copy(stressLevel = v.toInt()) } }, valueRange = 1f..10f, steps = 8)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NzTextField(form.sleepHours, { v -> vm.update { it.copy(sleepHours = v) } }, "Horas de sueño", Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                        NzTextField(form.heartRate, { v -> vm.update { it.copy(heartRate = v.filter(Char::isDigit)) } }, "Pulso (lpm)", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    }
                    NzTextField(form.notes, { v -> vm.update { it.copy(notes = v.take(300)) } }, "Notas (opcional)", singleLine = false, minLines = 2)
                    PrimaryButton("Guardar", vm::save, loading = form.saving)
                }
            }
            item { Text("Historial", style = MaterialTheme.typography.titleMedium) }
            item { if (history !is UiState.Success) StateContent(history, onRetry = { vm.load() }, emptyMessage = "Sin registros todavía.") { } }
            (history as? UiState.Success)?.data?.let { list ->
                items(list) { m ->
                    SectionCard {
                        Text(m.recordedAt?.let(DateTimeUtils::formatDateTime) ?: "Registro", fontWeight = FontWeight.SemiBold)
                        Text("Estrés ${m.stressLevel}/10" + (m.sleepHours?.let { " · Sueño ${it} h" } ?: "") + (m.heartRate?.let { " · $it lpm" } ?: ""))
                        m.notes?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
    }
}

private fun categoryIcon(key: String): androidx.compose.ui.graphics.vector.ImageVector = when (key.lowercase()) {
    "work" -> Icons.Outlined.Work
    "personal" -> Icons.Outlined.Person
    "health" -> Icons.Outlined.FavoriteBorder
    "family" -> Icons.Outlined.FamilyRestroom
    "financial" -> Icons.Outlined.Payments
    "social" -> Icons.Outlined.Groups
    else -> Icons.Outlined.Category
}
