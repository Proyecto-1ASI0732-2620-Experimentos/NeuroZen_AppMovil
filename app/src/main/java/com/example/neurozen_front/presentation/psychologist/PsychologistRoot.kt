package com.example.neurozen_front.presentation.psychologist

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.neurozen_front.domain.logic.Validators
import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.ProfessionalProfile
import com.example.neurozen_front.domain.model.UpdateProfessionalData
import com.example.neurozen_front.domain.model.VerificationStatus
import com.example.neurozen_front.domain.repository.ProfessionalRepository
import com.example.neurozen_front.domain.repository.SessionRepository
import com.example.neurozen_front.notifications.NotificationHelper
import com.example.neurozen_front.presentation.components.ErrorView
import com.example.neurozen_front.presentation.components.nzNavItemColors
import com.example.neurozen_front.presentation.components.InfoBanner
import com.example.neurozen_front.presentation.components.LoadingView
import com.example.neurozen_front.presentation.components.NzTextField
import com.example.neurozen_front.presentation.components.Pill
import com.example.neurozen_front.presentation.components.PrimaryButton
import com.example.neurozen_front.presentation.components.SectionCard
import com.example.neurozen_front.presentation.theme.NzColors
import com.example.neurozen_front.presentation.worker.ChangePasswordScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProRootState {
    data object Loading : ProRootState
    data class Error(val message: String) : ProRootState
    data object NoProfile : ProRootState
    data class Ready(val profile: ProfessionalProfile, val showWelcome: Boolean) : ProRootState
}

/** Holds the professional profile for the whole psychologist app (status gate + shared data). */
@HiltViewModel
class PsychologistViewModel @Inject constructor(
    private val professionals: ProfessionalRepository,
    private val sessions: SessionRepository
) : ViewModel() {
    private val _state = MutableStateFlow<ProRootState>(ProRootState.Loading)
    val state = _state.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.value = ProRootState.Loading
        _state.value = when (val r = professionals.getMyProfile()) {
            is Outcome.Success -> ProRootState.Ready(
                r.data,
                r.data.status == VerificationStatus.VERIFIED && sessions.shouldShowVerifiedWelcome(r.data.userId)
            )
            is Outcome.Failure -> if (r.error is AppError.NotFound) ProRootState.NoProfile else ProRootState.Error(r.error.message)
        }
    }

    fun dismissWelcome() = viewModelScope.launch {
        val ready = _state.value as? ProRootState.Ready ?: return@launch
        sessions.markVerifiedWelcomeShown(ready.profile.userId)
        _state.value = ready.copy(showWelcome = false)
    }

    /** PUT /professionals/me always sends the whole profile. */
    fun save(transform: (UpdateProfessionalData) -> UpdateProfessionalData, successMessage: String) {
        val ready = _state.value as? ProRootState.Ready ?: return
        val p = ready.profile
        val base = UpdateProfessionalData(p.name, p.specialty, p.licenseNumber, p.bio, p.experienceYears, p.price, p.schedule, p.availability)
        _saving.value = true
        viewModelScope.launch {
            when (val r = professionals.updateMyProfile(transform(base))) {
                is Outcome.Success -> { _state.value = ready.copy(profile = r.data); _message.value = successMessage }
                is Outcome.Failure -> _message.value = r.error.message
            }
            _saving.value = false
        }
    }

    fun clearMessage() { _message.value = null }
}

@Composable
fun PsychologistRoot(onLogout: () -> Unit, vm: PsychologistViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    when (val s = state) {
        ProRootState.Loading -> LoadingView("Cargando tu perfil profesional…")
        is ProRootState.Error -> Column { ErrorView(s.message, vm::load); TextButton(onClick = onLogout) { Text("Cerrar sesión") } }
        ProRootState.NoProfile -> Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Tu cuenta no tiene un perfil profesional. Regístrate como psicólogo desde la app o la web.", textAlign = TextAlign.Center)
            TextButton(onClick = onLogout) { Text("Cerrar sesión") }
        }
        is ProRootState.Ready -> when {
            s.profile.status != VerificationStatus.VERIFIED -> VerificationStatusScreen(s.profile, vm, onLogout)
            s.showWelcome -> VerifiedWelcomeScreen(s.profile, vm::dismissWelcome)
            else -> PsychologistNavHost(vm, onLogout)
        }
    }
}

// ---------- Not verified: pending or rejected with reason + re-submission ----------

@Composable
private fun VerificationStatusScreen(profile: ProfessionalProfile, vm: PsychologistViewModel, onLogout: () -> Unit) {
    val saving by vm.saving.collectAsState()
    val message by vm.message.collectAsState()
    var fullName by rememberSaveable { mutableStateOf(profile.name) }
    var license by rememberSaveable { mutableStateOf(profile.licenseNumber) }
    var specialty by rememberSaveable { mutableStateOf(profile.specialty) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Hola, ${profile.name.substringBefore(' ')}", style = MaterialTheme.typography.headlineSmall)
        Pill(profile.status.label, if (profile.status == VerificationStatus.REJECTED) NzColors.High else NzColors.Moderate)
        when (profile.status) {
            VerificationStatus.REJECTED -> {
                InfoBanner("Tu solicitud fue rechazada. Motivo: ${profile.rejectionReason ?: "no especificado"}", NzColors.High)
                Text("Corrige tus datos y vuelve a enviarla. Pasará otra vez a revisión.")
                NzTextField(fullName, { fullName = it }, "Nombre completo (como figura en el CPsP)")
                NzTextField(license, { license = it.filter(Char::isDigit) }, "N.° de colegiatura CPsP",
                    error = if (Validators.isValidLicense(license)) null else "Solo números")
                NzTextField(specialty, { specialty = it }, "Especialidad")
                PrimaryButton("Reenviar solicitud", onClick = {
                    vm.save({ it.copy(fullName = fullName, licenseNumber = license, specialty = specialty) }, "Solicitud reenviada. Está nuevamente en revisión.")
                }, loading = saving, enabled = Validators.isValidLicense(license) && fullName.isNotBlank())
            }
            else -> {
                InfoBanner("Estamos verificando tu colegiatura N.° ${profile.licenseNumber} en el portal del Colegio de Psicólogos del Perú. Mientras tanto no aparecerás en el directorio ni podrás recibir citas.")
                Text("Suele tomar 1 a 2 días hábiles. Vuelve a abrir la app para ver el resultado.")
                OutlinedButton(onClick = { vm.load() }) { Text("Actualizar estado") }
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        TextButton(onClick = onLogout) { Text("Cerrar sesión") }
    }
}

// ---------- Shown once after verification ----------

@Composable
private fun VerifiedWelcomeScreen(profile: ProfessionalProfile, onContinue: () -> Unit) {
    val labels = listOf("Biografía", "Años de experiencia", "Tarifa por sesión", "Horario de atención")
    val steps = profile.completedSteps
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Verified, contentDescription = null, tint = NzColors.Turquoise, modifier = Modifier.size(72.dp))
        Text("Psicólogo Verificado", style = MaterialTheme.typography.headlineSmall)
        Text("Tu colegiatura fue validada. Completa tu perfil para aparecer mejor en el directorio.", textAlign = TextAlign.Center)
        SectionCard {
            Text("${steps.count { it }} de 4 pasos completados", style = MaterialTheme.typography.titleMedium)
            labels.forEachIndexed { i, label ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(if (steps[i]) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked, contentDescription = if (steps[i]) "Completo" else "Pendiente",
                        tint = if (steps[i]) NzColors.Low else NzColors.Stone)
                    Text(label)
                }
            }
        }
        PrimaryButton("Ir a mi panel", onContinue)
    }
}

// ---------- Verified: tabs ----------

private data class ProTab(val route: String, val label: String, val icon: ImageVector)

private val proTabs = listOf(
    ProTab("pro_home", "Inicio", Icons.Outlined.Home),
    ProTab("pro_patients", "Pacientes", Icons.Outlined.Groups),
    ProTab("pro_schedule", "Agenda", Icons.Outlined.CalendarMonth),
    ProTab("pro_reports", "Informes", Icons.Outlined.Description),
    ProTab("pro_profile", "Perfil", Icons.Outlined.Person)
)

@Composable
private fun PsychologistNavHost(root: PsychologistViewModel, onLogout: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val state by root.state.collectAsState()
    val profile = (state as? ProRootState.Ready)?.profile ?: return
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.canNotify(context)) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val switchTab: (String) -> Unit = { r ->
        nav.navigate(r) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
    }
    Scaffold(bottomBar = {
        if (proTabs.any { it.route == route }) NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            proTabs.forEach { t ->
                NavigationBarItem(selected = route == t.route, onClick = { switchTab(t.route) }, icon = { Icon(t.icon, contentDescription = t.label) }, label = { Text(t.label) }, colors = nzNavItemColors(usePrimary = false))
            }
        }
    }) { padding ->
        NavHost(nav, startDestination = "pro_home", modifier = Modifier.padding(padding)) {
            composable("pro_home") { ProHomeScreen(profile, onOpenSchedule = { switchTab("pro_schedule") }, onEditProfile = { switchTab("pro_profile") }) }
            composable("pro_patients") { PatientsScreen(profile, onOpen = { nav.navigate("pro_patient/$it") }) }
            composable("pro_patient/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                PatientDetailScreen(profile, e.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() })
            }
            composable("pro_schedule") { ScheduleScreen(profile, root) }
            composable("pro_reports") { ReportsScreen(profile) }
            composable("pro_profile") { ProProfileScreen(profile, root, onChangePassword = { nav.navigate("pro_password") }, onLogout = onLogout, onOpenSchedule = { switchTab("pro_schedule") }) }
            composable("pro_password") { ChangePasswordScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
