package com.example.neurozen_front.presentation.worker

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import com.example.neurozen_front.presentation.components.DateBlock
import com.example.neurozen_front.presentation.components.GradientHeader
import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neurozen_front.domain.logic.AppointmentRules
import com.example.neurozen_front.domain.logic.DateTimeUtils
import com.example.neurozen_front.domain.model.Appointment
import com.example.neurozen_front.domain.model.AppointmentStatus
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.getOrNull
import com.example.neurozen_front.domain.repository.AppointmentRepository
import com.example.neurozen_front.domain.repository.ProfessionalRepository
import com.example.neurozen_front.domain.usecase.ShareReportUseCase
import com.example.neurozen_front.notifications.ReminderScheduler
import com.example.neurozen_front.presentation.components.ConfirmDialog
import com.example.neurozen_front.presentation.components.MessageChannel
import com.example.neurozen_front.presentation.components.NzTextField
import com.example.neurozen_front.presentation.components.SectionCard
import com.example.neurozen_front.presentation.components.StateContent
import com.example.neurozen_front.presentation.components.StatusPill
import com.example.neurozen_front.presentation.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class AppointmentItem(val appointment: Appointment, val professionalName: String)

@HiltViewModel
class MyAppointmentsViewModel @Inject constructor(
    private val appointments: AppointmentRepository,
    private val professionals: ProfessionalRepository,
    private val shareReport: ShareReportUseCase,
    private val reminders: ReminderScheduler
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<AppointmentItem>>>(UiState.Loading)
    val state = _state.asStateFlow()
    val messages = MessageChannel()
    private val names = mutableMapOf<Long, String>()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.value = UiState.Loading
        when (val r = appointments.getMine()) {
            is Outcome.Failure -> {
                // Offline: show the cached copy if there is one.
                val cached = appointments.observeCached().firstOrNull()
                _state.value = if (!cached.isNullOrEmpty()) {
                    messages.send("Sin conexión: mostrando tus citas guardadas.")
                    UiState.Success(cached.map { AppointmentItem(it, names[it.professionalId] ?: "Psicólogo") })
                } else UiState.Error(r.error.message)
            }
            is Outcome.Success -> {
                reminders.syncAppointments(r.data)
                r.data.map { it.professionalId }.distinct().filter { it !in names }.forEach { id ->
                    professionals.getById(id).getOrNull()?.let { names[id] = it.name }
                }
                _state.value = if (r.data.isEmpty()) UiState.Empty
                else UiState.Success(r.data.map { AppointmentItem(it, names[it.professionalId] ?: "Psicólogo") })
            }
        }
    }

    fun cancel(a: Appointment) = viewModelScope.launch {
        when (val r = appointments.updateStatus(a.id, AppointmentStatus.CANCELLED)) {
            is Outcome.Success -> { messages.send("Cita cancelada"); load() }
            is Outcome.Failure -> messages.send(r.error.message)
        }
    }

    fun share(a: Appointment, note: String) = viewModelScope.launch {
        when (val r = shareReport(a, note)) {
            is Outcome.Success -> messages.send("Informe compartido con tu psicólogo")
            is Outcome.Failure -> messages.send(r.error.message)
        }
    }

    fun review(a: Appointment, rating: Int, comment: String) = viewModelScope.launch {
        when (val r = professionals.addReview(a.professionalId, a.id, rating, comment)) {
            is Outcome.Success -> { messages.send("¡Gracias por tu reseña!"); load() }
            is Outcome.Failure -> messages.send(r.error.message)
        }
    }
}

private sealed interface AppointmentDialog {
    data class Cancel(val a: Appointment) : AppointmentDialog
    data class Share(val a: Appointment) : AppointmentDialog
    data class Review(val a: Appointment) : AppointmentDialog
}

@Composable
fun MyAppointmentsScreen(onFindProfessional: () -> Unit, vm: MyAppointmentsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<AppointmentDialog?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.messages.collect { snackbar.showSnackbar(it) } }
    val now = Instant.now()

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            GradientHeader("Mis Citas", "Tus sesiones con psicólogos verificados")
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Próximas") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Pasadas") })
            }
            StateContent(state, onRetry = { vm.load() }, emptyMessage = "Aún no tienes citas.", emptyAction = "Buscar psicólogo" to onFindProfessional) { items ->
                val list = if (tab == 0) items.filter { AppointmentRules.isUpcoming(it.appointment, now) }.sortedBy { it.appointment.dateTime }
                else items.filterNot { AppointmentRules.isUpcoming(it.appointment, now) }.sortedByDescending { it.appointment.dateTime }
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (list.isEmpty()) item { Text(if (tab == 0) "No tienes citas próximas." else "No tienes citas pasadas.") }
                    items(list, key = { it.appointment.id }) { item ->
                        val a = item.appointment
                        SectionCard {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                DateBlock(a.dateTime, highlighted = tab == 0)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(item.professionalName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(DateTimeUtils.formatDateTime(a.dateTime), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(4.dp))
                                    StatusPill(a.status)
                                }
                            }
                            val actions = AppointmentRules.canShareReport(a) || AppointmentRules.canReview(a) ||
                                AppointmentRules.canTransition(a.status, AppointmentStatus.CANCELLED, com.example.neurozen_front.domain.model.Role.USER)
                            if (actions) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (AppointmentRules.canShareReport(a)) OutlinedButton(onClick = { dialog = AppointmentDialog.Share(a) }) { Text("Compartir informe") }
                                    if (AppointmentRules.canReview(a)) Button(onClick = { dialog = AppointmentDialog.Review(a) }) { Text("Calificar sesión") }
                                    Spacer(Modifier.weight(1f))
                                    if (AppointmentRules.canTransition(a.status, AppointmentStatus.CANCELLED, com.example.neurozen_front.domain.model.Role.USER)) {
                                        TextButton(onClick = { dialog = AppointmentDialog.Cancel(a) }) { Text("Cancelar", color = MaterialTheme.colorScheme.error) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    when (val d = dialog) {
        is AppointmentDialog.Cancel -> ConfirmDialog("Cancelar cita", "¿Seguro que quieres cancelar esta cita?", "Cancelar cita", { vm.cancel(d.a) }, { dialog = null }, destructive = true)
        is AppointmentDialog.Share -> ShareReportDialog(onDismiss = { dialog = null }, onShare = { note -> vm.share(d.a, note); dialog = null })
        is AppointmentDialog.Review -> ReviewDialog(onDismiss = { dialog = null }, onSend = { r, c -> vm.review(d.a, r, c); dialog = null })
        null -> Unit
    }
}

/** Sensitive content: FLAG_SECURE while the dialog is open blocks screenshots. */
@Composable
private fun SecureWindow() {
    val activity = LocalContext.current as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

@Composable
private fun ShareReportDialog(onDismiss: () -> Unit, onShare: (String) -> Unit) {
    SecureWindow()
    var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compartir informe de progreso") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Se enviará a tu psicólogo un resumen con tu nivel actual y promedio de estrés, cambio semanal, horas pico, número de registros, categoría principal y tu último test. No se envían las descripciones de tus registros.")
                NzTextField(note, { note = it.take(500) }, "Nota para tu psicólogo (opcional)", singleLine = false, minLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onShare(note) }) { Text("Compartir") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ReviewDialog(onDismiss: () -> Unit, onSend: (Int, String) -> Unit) {
    var rating by rememberSaveable { mutableIntStateOf(5) }
    var comment by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Califica tu sesión") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row {
                    (1..5).forEach { i -> TextButton(onClick = { rating = i }) { Text(if (i <= rating) "★" else "☆", style = MaterialTheme.typography.headlineSmall) } }
                }
                NzTextField(comment, { comment = it.take(500) }, "Comentario (opcional)", singleLine = false, minLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onSend(rating, comment) }) { Text("Enviar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
