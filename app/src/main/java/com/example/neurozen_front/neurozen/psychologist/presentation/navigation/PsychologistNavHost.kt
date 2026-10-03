package com.example.neurozen_front.neurozen.psychologist.presentation.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.neurozen_front.R
import com.example.neurozen_front.neurozen.data.local.AppointmentEntity
import com.example.neurozen_front.neurozen.data.network.ResourceLibraryItem
import com.example.neurozen_front.neurozen.data.session.UserSession
import com.example.neurozen_front.neurozen.home.presentation.psychologists.PsychologistsViewModel
import com.example.neurozen_front.neurozen.home.presentation.psychologists.VideoCallScreen
import com.example.neurozen_front.neurozen.psychologist.presentation.zenbot.ZenBotProScreen
import java.text.SimpleDateFormat
import java.util.*

enum class PsychologistTab(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Agenda("Agenda", Icons.Default.DateRange),
    ZenBotPro("ZenBot Pro", Icons.Default.Psychology),
    Recursos("Recursos", Icons.Default.LibraryBooks),
    Perfil("Mi Perfil", Icons.Default.AccountCircle)
}

@Composable
fun PsychologistNavHost(
    onLogout: () -> Unit = {}
) {
    val selectedTab = remember { mutableStateOf(PsychologistTab.Agenda) }
    val psychViewModel: PsychologistsViewModel = hiltViewModel()
    val appointments by psychViewModel.appointments.collectAsState()
    var activeCallAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }

    val session = UserSession.state.collectAsState().value
    val doctorName = session.name ?: "Carlos Rodríguez"

    com.example.neurozen_front.ui.theme.NeurozenTheme {
        if (activeCallAppointment != null) {
            VideoCallScreen(
                appointment = activeCallAppointment!!,
                onHangUp = { activeCallAppointment = null }
            )
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        PsychologistTab.entries.forEach { tab ->
                            val isSelected = selectedTab.value == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab.value = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    when (selectedTab.value) {
                        PsychologistTab.Agenda -> PsychologistAgendaScreen(
                            appointments = appointments,
                            doctorName = doctorName,
                            onStartCall = { activeCallAppointment = it }
                        )
                        PsychologistTab.ZenBotPro -> ZenBotProScreen()
                        PsychologistTab.Recursos -> ResourceLibraryScreen()
                        PsychologistTab.Perfil -> PsychologistProfileScreen(
                            doctorName = doctorName,
                            doctorEmail = session.email ?: "carlos.rodriguez@neurozen.com",
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PsychologistAgendaScreen(
    appointments: List<AppointmentEntity>,
    doctorName: String,
    onStartCall: (AppointmentEntity) -> Unit
) {
    // Filtrar citas para el doctor logueado (o mostrar todas en modo demo)
    val doctorAppointments = appointments.filter { app ->
        app.psychologistName.contains(doctorName.split(" ").firstOrNull() ?: doctorName, ignoreCase = true) || appointments.size <= 3
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Portal del Psicólogo 🩺", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Text("Hola, $doctorName", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tienes ${doctorAppointments.size} cita(s) agendada(s) por pacientes.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBadge("Pacientes", "${doctorAppointments.size}", "👤", Modifier.weight(1f))
                StatBadge("Valoración", "4.9 ★", "⭐", Modifier.weight(1f))
                StatBadge("Consultas", "12", "📈", Modifier.weight(1f))
            }
        }

        item {
            Text("Citas de Pacientes Reservadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (doctorAppointments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📅", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No tienes citas pendientes por ahora", fontWeight = FontWeight.SemiBold)
                        Text("Cuando un cliente agende una cita contigo, aparecerá aquí automáticamente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        items(doctorAppointments) { appointment ->
            val date = Date(appointment.dateMillis)
            val dateFormatted = SimpleDateFormat("EEEE d 'de' MMMM", Locale.US).format(date).replaceFirstChar { it.uppercase() }
            val timeFormatted = SimpleDateFormat("hh:mm a", Locale.US).format(date)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👤", fontSize = 20.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Paciente: joao", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Especialidad: ${appointment.psychologistSpecialty}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("$dateFormatted a las $timeFormatted", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { onStartCall(appointment) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.VideoCall, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Iniciar Videollamada con joao")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String, icon: String, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 20.sp)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ResourceLibraryScreen() {
    var showPublishDialog by remember { mutableStateOf(false) }

    val mockResources = remember {
        mutableStateListOf(
            ResourceLibraryItem(1, "Guía de Respiración Diafragmática", "Exercise", "Ansiedad", "Ejercicio práctico para reducir hiperventilación en ataques de pánico.", "https://neurozen.app/resources/respiracion.pdf", "", "Dr. NeuroZen"),
            ResourceLibraryItem(2, "Registro de Pensamientos Automáticos (TCC)", "Article", "Mindfulness", "Plantilla en PDF para reestructuración cognitiva en consulta.", "https://neurozen.app/resources/tcc_registro.pdf", "", "Equipo Clínico"),
            ResourceLibraryItem(3, "Higiene del Sueño y Descanso", "Video", "Sueño", "Recomendaciones psicoeducativas para conciliar el sueño.", "https://neurozen.app/resources/suno.mp4", "", "Dra. Ana Torres")
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Biblioteca de Recursos 📚", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Herramientas psicoeducativas para consulta", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { showPublishDialog = true }) {
                Text("+ Publicar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(mockResources) { res ->
                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                Text(res.category, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Text(res.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(res.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(res.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showPublishDialog) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Ansiedad") }
        var description by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPublishDialog = false },
            title = { Text("Publicar Recurso Terapéutico") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título del recurso") })
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Categoría (ej: Ansiedad, Sueño)") })
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descripción o instrucciones") }, minLines = 3)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        mockResources.add(ResourceLibraryItem(mockResources.size + 1, title, "Article", category, description, "", "", "Tú"))
                    }
                    showPublishDialog = false
                }) {
                    Text("Publicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublishDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun PsychologistProfileScreen(
    doctorName: String,
    doctorEmail: String,
    onLogout: () -> Unit
) {
    // Mapeo dinámico de foto y precio según el especialista logueado
    val (avatarDrawable, priceText) = when {
        doctorName.contains("Ana", ignoreCase = true) -> Pair(R.drawable.psicologo_ana, "S/ 50.00")
        doctorName.contains("Maria", ignoreCase = true) || doctorName.contains("María", ignoreCase = true) -> Pair(R.drawable.psicologo_maria, "S/ 55.00")
        else -> Pair(R.drawable.psicologo_carlos, "S/ 45.00")
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Image(
                painter = painterResource(id = avatarDrawable),
                contentDescription = "Foto de $doctorName",
                modifier = Modifier.size(80.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                contentScale = ContentScale.Crop
            )
            Column {
                Text(doctorName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(doctorEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Badge(containerColor = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Especialista Certificado", color = Color.White)
                }
            }
        }

        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Ajustes Profesionales", fontWeight = FontWeight.Bold)
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Precio por consulta")
                    Text(priceText, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Estado de disponibilidad")
                    Text("En línea / Activo 🟢", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Cerrar Sesión")
        }
    }
}
