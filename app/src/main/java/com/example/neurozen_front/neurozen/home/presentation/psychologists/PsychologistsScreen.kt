package com.example.neurozen_front.neurozen.home.presentation.psychologists

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.neurozen_front.R
import com.example.neurozen_front.neurozen.data.local.AppointmentEntity
import com.example.neurozen_front.neurozen.data.network.ProfessionalResource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsychologistsScreen(
    viewModel: PsychologistsViewModel = hiltViewModel()
) {
    var selectedPsychologist by remember { mutableStateOf<ProfessionalResource?>(null) }
    var showVideoCall by remember { mutableStateOf<AppointmentEntity?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    val appointments by viewModel.appointments.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (showVideoCall != null) {
        VideoCallScreen(
            appointment = showVideoCall!!,
            onHangUp = { showVideoCall = null }
        )
    } else {
        var showBookingDialog by remember { mutableStateOf(false) }

        if (showBookingDialog) {
            var appointmentType by remember { mutableIntStateOf(1) } // Default: Consulta Inicial
            var additionalNotes by remember { mutableStateOf("") }
            
            val datePickerState = rememberDatePickerState()
            val timePickerState = rememberTimePickerState(is24Hour = false)
            var currentStep by remember { mutableIntStateOf(0) } // 0: Details, 1: Date, 2: Time

            when (currentStep) {
                0 -> {
                    AlertDialog(
                        onDismissRequest = { showBookingDialog = false },
                        title = { Text("Detalles de la Cita", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Selecciona el tipo de cita:", style = MaterialTheme.typography.bodyMedium)
                                
                                val types = listOf("Consulta Inicial", "Terapia Individual", "Sesión de Seguimiento")
                                types.forEachIndexed { index, label ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().clickable { appointmentType = index + 1 }
                                    ) {
                                        RadioButton(selected = appointmentType == index + 1, onClick = { appointmentType = index + 1 })
                                        Text(label)
                                    }
                                }
                                
                                OutlinedTextField(
                                    value = additionalNotes,
                                    onValueChange = { additionalNotes = it },
                                    label = { Text("Notas adicionales (opcional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = { currentStep = 1 }) {
                                Text("Siguiente")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBookingDialog = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
                1 -> {
                    DatePickerDialog(
                        onDismissRequest = { showBookingDialog = false },
                        confirmButton = {
                            TextButton(onClick = { currentStep = 2 }) {
                                Text("Siguiente: Hora")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { currentStep = 0 }) {
                                Text("Atrás")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
                2 -> {
                    AlertDialog(
                        onDismissRequest = { showBookingDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val dateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                                val calendar = Calendar.getInstance().apply {
                                    timeInMillis = dateMillis
                                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                    set(Calendar.MINUTE, timePickerState.minute)
                                }
                                selectedPsychologist?.let { psycho ->
                                    viewModel.scheduleAppointment(
                                        professional = psycho,
                                        dateMillis = calendar.timeInMillis,
                                        type = appointmentType,
                                        notes = additionalNotes
                                    )
                                }
                                showBookingDialog = false
                            }) {
                                Text("Confirmar Cita")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { currentStep = 1 }) {
                                Text("Atrás")
                            }
                        },
                        title = { Text("Selecciona la hora") },
                        text = {
                            TimePicker(state = timePickerState)
                        }
                    )
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                                )
                            )
                            .padding(top = 40.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
                    ) {
                        Column {
                            Text(
                                text = "Centro de Ayuda",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Profesionales listos para escucharte",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                    
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("Explorar", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { 
                                BadgedBox(badge = {
                                    if (appointments.isNotEmpty()) {
                                        Badge { Text(appointments.size.toString()) }
                                    }
                                }) {
                                    Text("Mis Citas", fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                when (selectedTabIndex) {
                    0 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text(
                                    "Especialistas destacados",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            items(uiState.professionals) { professional ->
                                PsychologistCard(
                                    professional = professional,
                                    onBookClick = {
                                        selectedPsychologist = professional
                                        showBookingDialog = true
                                    }
                                )
                            }

                            if (!uiState.isLoading && uiState.professionals.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("No se encontraron especialistas", color = MaterialTheme.colorScheme.outline)
                                        Button(onClick = { viewModel.loadProfessionals() }, modifier = Modifier.padding(top = 16.dp)) {
                                            Text("Reintentar carga")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        if (appointments.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("No tienes citas agendadas", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                                    TextButton(onClick = { selectedTabIndex = 0 }) {
                                        Text("Ver especialistas")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(appointments) { appointment ->
                                    AppointmentCard(
                                        appointment = appointment,
                                        onStartCall = { showVideoCall = it },
                                        onDelete = { viewModel.deleteAppointment(it) }
                                    )
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
fun AppointmentCard(
    appointment: AppointmentEntity,
    onStartCall: (AppointmentEntity) -> Unit,
    onDelete: (AppointmentEntity) -> Unit
) {
    val date = Date(appointment.dateMillis)
    val dateStr = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("es", "PE")).format(date)
    val timeStr = SimpleDateFormat("hh:mm a", Locale("es", "PE")).format(date)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(appointment.psychologistName.first().toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(appointment.psychologistName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(appointment.psychologistSpecialty, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Cancelar Cita", color = Color.Red) },
                            onClick = {
                                onDelete(appointment)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                        )
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text("${dateStr.replaceFirstChar { it.uppercase() }} • $timeStr", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = { onStartCall(appointment) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.VideoCall, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unirse a Videollamada")
            }
        }
    }
}

@Composable
fun PsychologistCard(professional: ProfessionalResource, onBookClick: () -> Unit) {
    // Mapeo dinámico a imágenes locales enviadas por el usuario
    val localImage = when {
        professional.firstName.contains("Ana", ignoreCase = true) -> R.drawable.psicologo_ana
        professional.firstName.contains("Maria", ignoreCase = true) -> R.drawable.psicologo_maria
        professional.firstName.contains("Carlos", ignoreCase = true) -> R.drawable.psicologo_carlos
        else -> null
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = localImage ?: if (professional.imageUrl.isBlank()) R.drawable.neurozen_placeholder_avatar else professional.imageUrl,
                contentDescription = "${professional.firstName} ${professional.lastName}",
                placeholder = painterResource(R.drawable.neurozen_placeholder_avatar),
                error = painterResource(R.drawable.neurozen_placeholder_avatar),
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${professional.firstName} ${professional.lastName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = professional.specialization,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                if (!professional.bio.isNullOrBlank()) {
                    Text(
                        text = professional.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                            Text(" ${professional.rating ?: 5.0}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        if (professional.price != null && professional.price > 0) {
                            Text("S/ ${professional.price}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Button(
                        onClick = onBookClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Agendar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VideoCallScreen(
    appointment: AppointmentEntity,
    onHangUp: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
        // Simulación de video del psicólogo (Pantalla completa)
        Box(modifier = Modifier.fillMaxSize()) {
            val imageRes = when {
                appointment.psychologistName.contains("Ana", ignoreCase = true) -> R.drawable.psicologo_ana
                appointment.psychologistName.contains("Maria", ignoreCase = true) -> R.drawable.psicologo_maria
                appointment.psychologistName.contains("Carlos", ignoreCase = true) -> R.drawable.psicologo_carlos
                else -> R.drawable.neurozen_placeholder_avatar
            }
            
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.8f
            )
            
            // Gradiente para visibilidad
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)), startY = 500f)))
        }

        // Simulación de mi video (Miniatura arriba derecha)
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .size(100.dp, 150.dp)
                .align(Alignment.TopEnd),
            shape = RoundedCornerShape(16.dp),
            color = Color.DarkGray,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
        ) {
            Image(
                painter = painterResource(id = R.drawable.usuario_demo),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }

        // Información arriba izquierda
        Column(modifier = Modifier.padding(24.dp).align(Alignment.TopStart)) {
            Text(appointment.psychologistName, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Sesión de Terapia • Activa", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
        }

        // Controles abajo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)
        ) {
            VideoControlBtn(icon = Icons.Default.Mic, active = true)
            VideoControlBtn(icon = Icons.Default.Videocam, active = true)
            
            FloatingActionButton(
                onClick = onHangUp,
                containerColor = Color.Red,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = "Colgar", modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
fun VideoControlBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean) {
    Surface(
        onClick = { /* Toggle */ },
        shape = CircleShape,
        color = if (active) Color.White.copy(alpha = 0.2f) else Color.White,
        modifier = Modifier.size(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) Color.White else Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
