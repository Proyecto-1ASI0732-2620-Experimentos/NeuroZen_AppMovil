package com.example.neurozen_front.presentation.worker

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.WorkHistory
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.example.neurozen_front.presentation.components.Accent
import com.example.neurozen_front.presentation.components.Avatar
import com.example.neurozen_front.presentation.components.CompletionCard
import com.example.neurozen_front.presentation.components.GradientHeader
import com.example.neurozen_front.presentation.components.HeaderStat
import com.example.neurozen_front.presentation.components.ListRow
import com.example.neurozen_front.presentation.components.SettingsGroup
import com.example.neurozen_front.presentation.components.GroupDivider
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neurozen_front.domain.logic.CardValidator
import com.example.neurozen_front.domain.logic.Catalogs
import com.example.neurozen_front.domain.logic.Validators
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.PaymentData
import com.example.neurozen_front.domain.model.Subscription
import com.example.neurozen_front.domain.model.UpdateProfileData
import com.example.neurozen_front.domain.model.UserProfile
import com.example.neurozen_front.domain.repository.SubscriptionRepository
import com.example.neurozen_front.domain.repository.UserRepository
import com.example.neurozen_front.domain.usecase.LogoutUseCase
import com.example.neurozen_front.presentation.components.ConfirmDialog
import com.example.neurozen_front.presentation.components.InfoBanner
import com.example.neurozen_front.presentation.components.MessageChannel
import com.example.neurozen_front.presentation.components.NzTextField
import com.example.neurozen_front.presentation.components.NzTopBar
import com.example.neurozen_front.presentation.components.PasswordField
import com.example.neurozen_front.presentation.components.PrimaryButton
import com.example.neurozen_front.presentation.components.SectionCard
import com.example.neurozen_front.presentation.components.StateContent
import com.example.neurozen_front.presentation.components.UiState
import com.example.neurozen_front.presentation.components.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

val AGE_RANGES = listOf("18-24", "25-34", "35-44", "45-54", "55+")
val WORK_TYPES = listOf("Presencial", "Remoto", "Híbrido")

data class ProfileOverview(
    val user: UserProfile,
    val triggersCount: Int,
    val testsCount: Int,
    val lastLevel: com.example.neurozen_front.domain.model.StressLevel?,
    val breathingCount: Int,
    val subscription: Subscription?
) {
    val completedFields: Int
        get() = listOf(user.phoneNumber, user.ageRange, user.occupation, user.workType).count { !it.isNullOrBlank() }
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val users: UserRepository,
    private val stress: com.example.neurozen_front.domain.repository.StressRepository,
    private val subscriptions: SubscriptionRepository,
    private val wellness: com.example.neurozen_front.domain.repository.WellnessRepository,
    private val logout: LogoutUseCase
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<ProfileOverview>>(UiState.Loading)
    val state = _state.asStateFlow()
    val messages = MessageChannel()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.value = UiState.Loading
        when (val user = users.getMe()) {
            is Outcome.Failure -> _state.value = UiState.Error(user.error.message)
            is Outcome.Success -> {
                // Secondary data must never block the profile.
                val triggers = stress.getTriggers()
                val tests = stress.getAssessments()
                val plan = subscriptions.getMine()
                val breathing = wellness.observeBreathingSessions().first().size
                val testList = (tests as? Outcome.Success)?.data.orEmpty()
                _state.value = UiState.Success(
                    ProfileOverview(
                        user = user.data,
                        triggersCount = (triggers as? Outcome.Success)?.data?.size ?: 0,
                        testsCount = testList.size,
                        lastLevel = testList.firstOrNull()?.level,
                        breathingCount = breathing,
                        subscription = (plan as? Outcome.Success)?.data
                    )
                )
            }
        }
    }

    /** Ley 29733: deletes the account and personal data on the server, then local data. */
    fun deleteAccount() = viewModelScope.launch {
        when (val r = users.deleteMe()) {
            is Outcome.Success -> logout()
            is Outcome.Failure -> messages.send(r.error.message)
        }
    }
}

@Composable
fun ProfileScreen(onNavigate: (String) -> Unit, onLogout: () -> Unit, vm: ProfileViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    LaunchedEffect(Unit) { vm.messages.messages.collect { snackbar.showSnackbar(it) } }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.padding(padding)) {
            StateContent(state, onRetry = { vm.load() }) { data ->
                val user = data.user
                val plan = data.subscription?.takeIf { it.isActive }?.let { s -> Catalogs.PLANS.firstOrNull { it.id == s.planId }?.name } ?: "Básico"
                LazyColumn(contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    item {
                        GradientHeader(
                            title = "Mi perfil", subtitle = "Tu espacio personal en NeuroZen",
                            bottom = {
                                Spacer(Modifier.height(18.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Avatar(user.fullName, size = 72, ring = true)
                                    Column(Modifier.weight(1f)) {
                                        Text(user.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(user.email, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "Plan $plan", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                                            color = com.example.neurozen_front.presentation.theme.Forest900,
                                            modifier = Modifier.clip(RoundedCornerShape(50)).background(com.example.neurozen_front.presentation.theme.Mint300).padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(18.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    HeaderStat(data.triggersCount.toString(), "Registros", Modifier.weight(1f))
                                    HeaderStat(data.testsCount.toString(), "Tests", Modifier.weight(1f))
                                    HeaderStat(data.breathingCount.toString(), "Respiraciones", Modifier.weight(1f))
                                    HeaderStat(data.lastLevel?.label ?: "—", "Último nivel", Modifier.weight(1f))
                                }
                            }
                        )
                    }
                    if (data.completedFields < 4) item {
                        CompletionCard(
                            "Completa tu perfil", data.completedFields, 4,
                            "Edad, ocupación, modalidad y teléfono nos ayudan a personalizar tus recomendaciones.",
                            Modifier.padding(horizontal = 20.dp)
                        ) { onNavigate("profile_edit") }
                    }
                    item {
                        SettingsGroup("Información personal", Modifier.padding(horizontal = 20.dp)) {
                            ListRow(Icons.Outlined.Person, "Datos personales", listOfNotNull(user.occupation, user.workType).joinToString(" · ").ifBlank { "Nombre, correo, teléfono" }) { onNavigate("profile_edit") }
                            GroupDivider()
                            ListRow(Icons.Outlined.Cake, "Rango de edad", trailingText = user.ageRange ?: "Sin definir") { onNavigate("profile_edit") }
                            GroupDivider()
                            ListRow(Icons.Outlined.Phone, "Teléfono", trailingText = user.phoneNumber ?: "Sin definir") { onNavigate("profile_edit") }
                        }
                    }
                    item {
                        SettingsGroup("Bienestar", Modifier.padding(horizontal = 20.dp)) {
                            ListRow(Icons.Outlined.NotificationsActive, "Recordatorios", "Pausas activas y ergonomía", tint = Accent.Teal) { onNavigate("breaks") }
                            GroupDivider()
                            ListRow(Icons.Outlined.Insights, "Historial de tests", "Tu evolución de estrés", tint = Accent.Blue) { onNavigate("assessment") }
                            GroupDivider()
                            ListRow(Icons.Outlined.WorkHistory, "Carga laboral", "Horas trabajadas por semana", tint = Accent.Amber) { onNavigate("workload") }
                            GroupDivider()
                            ListRow(Icons.Outlined.EmojiEvents, "Desafíos", "Tus hábitos antiestrés", tint = Accent.Purple) { onNavigate("challenges") }
                        }
                    }
                    item {
                        SettingsGroup("Cuenta y seguridad", Modifier.padding(horizontal = 20.dp)) {
                            ListRow(Icons.Outlined.WorkspacePremium, "Suscripción", trailingText = plan, tint = Accent.Amber) { onNavigate("subscription") }
                            GroupDivider()
                            ListRow(Icons.Outlined.Lock, "Cambiar contraseña") { onNavigate("change_password") }
                            GroupDivider()
                            ListRow(Icons.Outlined.Shield, "Privacidad de datos", "Tus datos se cifran y solo compartes informes si tú lo decides", showChevron = false)
                        }
                    }
                    item {
                        SettingsGroup("Ayuda", Modifier.padding(horizontal = 20.dp)) {
                            ListRow(Icons.Outlined.SupportAgent, "Línea 113 · opción 5", "Salud mental gratuita (MINSA), 24 horas", tint = Accent.Coral) { uri.openUri("tel:113") }
                            GroupDivider()
                            ListRow(Icons.Outlined.Spa, "Hablar con ZenBot", "Sugerencias rápidas de bienestar", tint = Accent.Emerald) { onNavigate("zenbot") }
                        }
                    }
                    item {
                        SettingsGroup("Sesión", Modifier.padding(horizontal = 20.dp)) {
                            ListRow(Icons.AutoMirrored.Outlined.Logout, "Cerrar sesión", showChevron = false) { confirmLogout = true }
                            GroupDivider()
                            ListRow(Icons.Outlined.DeleteForever, "Eliminar mi cuenta y mis datos", destructive = true, showChevron = false) { confirmDelete = true }
                        }
                    }
                    item {
                        Text(
                            "NeuroZen v${com.example.neurozen_front.BuildConfig.VERSION_NAME} · Hecho en Perú 🇵🇪",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
    if (confirmLogout) ConfirmDialog("Cerrar sesión", "Se borrarán los datos guardados en este teléfono y tus recordatorios.", "Cerrar sesión", onLogout, { confirmLogout = false })
    if (confirmDelete) ConfirmDialog(
        "Eliminar cuenta", "Se eliminarán tu cuenta y tus datos personales de forma permanente (Ley 29733). Esta acción no se puede deshacer.",
        "Eliminar", vm::deleteAccount, { confirmDelete = false }, destructive = true
    )
}

// ---------- Edit profile / onboarding fields ----------

data class ProfileForm(
    val fullName: String = "", val email: String = "", val phone: String = "",
    val ageRange: String? = null, val occupation: String = "", val workType: String? = null,
    val loaded: Boolean = false, val saving: Boolean = false, val saved: Boolean = false, val error: String? = null
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(private val users: UserRepository) : ViewModel() {
    private val _form = MutableStateFlow(ProfileForm())
    val form = _form.asStateFlow()

    init {
        viewModelScope.launch {
            when (val r = users.getMe()) {
                is Outcome.Success -> r.data.let { u ->
                    _form.value = ProfileForm(u.fullName, u.email, u.phoneNumber.orEmpty(), u.ageRange, u.occupation.orEmpty(), u.workType, loaded = true)
                }
                is Outcome.Failure -> _form.update { it.copy(loaded = true, error = r.error.message) }
            }
        }
    }

    fun update(transform: (ProfileForm) -> ProfileForm) = _form.update { transform(it).copy(error = null) }

    fun save() {
        val f = _form.value
        val error = when {
            f.fullName.isBlank() -> "Ingresa tu nombre"
            !Validators.isValidEmail(f.email) -> "Correo inválido"
            f.phone.isNotBlank() && !Regex("""^\+?\d{9,12}$""").matches(f.phone) -> "Teléfono inválido"
            else -> null
        }
        if (error != null) { _form.update { it.copy(error = error) }; return }
        _form.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val r = users.updateMe(UpdateProfileData(f.fullName, f.email, f.phone, f.ageRange, f.occupation, f.workType))) {
                is Outcome.Success -> _form.update { it.copy(saving = false, saved = true) }
                is Outcome.Failure -> _form.update { it.copy(saving = false, error = r.error.message) }
            }
        }
    }
}

@Composable
fun ProfileEditScreen(isOnboarding: Boolean, onDone: () -> Unit, onBack: () -> Unit, vm: ProfileEditViewModel = hiltViewModel()) {
    val f by vm.form.collectAsState()
    LaunchedEffect(f.saved) { if (f.saved) onDone() }
    Scaffold(topBar = { NzTopBar(if (isOnboarding) "Cuéntanos de ti" else "Mis datos", if (isOnboarding) null else onBack) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isOnboarding) {
                Text("¡Bienvenido/a a NeuroZen! 🌿", style = MaterialTheme.typography.headlineSmall)
                Text("Estos datos son opcionales y nos ayudan a personalizar tus recomendaciones. Luego harás un test inicial de 3 minutos.")
            }
            if (!isOnboarding) {
                NzTextField(f.fullName, { v -> vm.update { it.copy(fullName = v) } }, "Nombre completo")
                NzTextField(f.email, { v -> vm.update { it.copy(email = v) } }, "Correo", keyboardType = KeyboardType.Email)
                NzTextField(f.phone, { v -> vm.update { it.copy(phone = v) } }, "Teléfono (opcional)", keyboardType = KeyboardType.Phone)
            }
            Text("Edad", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(AGE_RANGES) { a -> FilterChip(selected = f.ageRange == a, onClick = { vm.update { it.copy(ageRange = if (it.ageRange == a) null else a) } }, label = { Text(a) }) }
            }
            NzTextField(f.occupation, { v -> vm.update { it.copy(occupation = v.take(80)) } }, "Ocupación (opcional)")
            Text("Modalidad de trabajo", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WORK_TYPES.forEach { w -> FilterChip(selected = f.workType == w, onClick = { vm.update { it.copy(workType = if (it.workType == w) null else w) } }, label = { Text(w) }) }
            }
            f.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PrimaryButton(if (isOnboarding) "Continuar al test" else "Guardar", vm::save, loading = f.saving, enabled = f.loaded)
            if (isOnboarding) TextButton(onClick = onDone) { Text("Omitir por ahora") }
        }
    }
}

// ---------- Change password ----------

data class ChangePasswordForm(val current: String = "", val new: String = "", val confirm: String = "", val loading: Boolean = false, val error: String? = null, val done: Boolean = false)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(private val users: UserRepository) : ViewModel() {
    private val _form = MutableStateFlow(ChangePasswordForm())
    val form = _form.asStateFlow()
    fun update(t: (ChangePasswordForm) -> ChangePasswordForm) = _form.update { t(it).copy(error = null) }
    fun save() {
        val f = _form.value
        val error = when {
            f.current.isBlank() -> "Ingresa tu contraseña actual"
            Validators.passwordError(f.new) != null -> Validators.passwordError(f.new)
            f.new == f.current -> "La nueva contraseña debe ser distinta"
            f.new != f.confirm -> "Las contraseñas no coinciden"
            else -> null
        }
        if (error != null) { _form.update { it.copy(error = error) }; return }
        _form.update { it.copy(loading = true) }
        viewModelScope.launch {
            when (val r = users.changePassword(f.current, f.new)) {
                is Outcome.Success -> _form.value = ChangePasswordForm(done = true)
                is Outcome.Failure -> _form.update { it.copy(loading = false, error = r.error.message) }
            }
        }
    }
}

@Composable
fun ChangePasswordScreen(onBack: () -> Unit, vm: ChangePasswordViewModel = hiltViewModel()) {
    val f by vm.form.collectAsState()
    Scaffold(topBar = { NzTopBar("Cambiar contraseña", onBack) }) { padding ->
        Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (f.done) {
                InfoBanner("Tu contraseña fue actualizada.")
                PrimaryButton("Volver", onBack)
            } else {
                PasswordField(f.current, { v -> vm.update { it.copy(current = v) } }, "Contraseña actual")
                PasswordField(f.new, { v -> vm.update { it.copy(new = v) } }, "Nueva contraseña")
                PasswordField(f.confirm, { v -> vm.update { it.copy(confirm = v) } }, "Confirmar nueva contraseña")
                f.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                PrimaryButton("Guardar", vm::save, loading = f.loading)
            }
        }
    }
}

// ---------- Subscription (card validated locally; only last 4 digits are kept by the backend) ----------

data class PaymentForm(
    val planId: Int? = null, val firstName: String = "", val lastName: String = "", val email: String = "",
    val card: String = "", val expiration: String = "", val cvv: String = "", val paying: Boolean = false, val error: String? = null
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(private val subscriptions: SubscriptionRepository) : ViewModel() {
    private val _current = MutableStateFlow<UiState<Subscription?>>(UiState.Loading)
    val current = _current.asStateFlow()
    private val _form = MutableStateFlow(PaymentForm())
    val form = _form.asStateFlow()
    val messages = MessageChannel()

    init { load() }

    fun load() = viewModelScope.launch {
        _current.value = when (val r = subscriptions.getMine()) {
            is Outcome.Success -> UiState.Success(r.data)
            is Outcome.Failure -> UiState.Error(r.error.message)
        }
    }

    fun update(t: (PaymentForm) -> PaymentForm) = _form.update { t(it).copy(error = null) }

    fun pay() {
        val f = _form.value
        val error = when {
            f.planId == null -> "Elige un plan"
            f.firstName.isBlank() || f.lastName.isBlank() -> "Ingresa el nombre del titular"
            !Validators.isValidEmail(f.email) -> "Correo inválido"
            !CardValidator.isValidNumber(f.card) -> "Número de tarjeta inválido"
            !CardValidator.isValidExpiration(f.expiration) -> "Vencimiento inválido (MM/AA)"
            !CardValidator.isValidCvv(f.cvv) -> "CVV inválido"
            else -> null
        }
        if (error != null) { _form.update { it.copy(error = error) }; return }
        _form.update { it.copy(paying = true) }
        viewModelScope.launch {
            val r = subscriptions.subscribe(PaymentData(f.planId!!, f.firstName, f.lastName, f.email, f.card, f.expiration, f.cvv))
            // Card data never stays in memory after sending.
            _form.update { it.copy(paying = false, card = "", cvv = "", expiration = "") }
            when (r) {
                is Outcome.Success -> { messages.send("Suscripción activada"); _current.value = UiState.Success(r.data) }
                is Outcome.Failure -> _form.update { it.copy(error = r.error.message) }
            }
        }
    }
}

@Composable
fun SubscriptionScreen(onBack: () -> Unit, vm: SubscriptionViewModel = hiltViewModel()) {
    val current by vm.current.collectAsState()
    val f by vm.form.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.messages.collect { snackbar.showSnackbar(it) } }
    Scaffold(topBar = { NzTopBar("Suscripción", onBack) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (val c = current) {
                UiState.Loading -> Text("Cargando tu plan…")
                is UiState.Error -> Text(c.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> c.data?.let { s ->
                    val plan = Catalogs.PLANS.firstOrNull { it.id == s.planId }
                    InfoBanner("Plan actual: ${plan?.name ?: "#${s.planId}"} · ${if (s.isActive) "Activo" else "Inactivo"}" + (s.cardLast4?.let { " · Tarjeta •••• $it" } ?: ""))
                } ?: Text("Aún no tienes un plan.")
                UiState.Empty -> Unit
            }
            Catalogs.PLANS.forEach { plan ->
                val selected = f.planId == plan.id
                SectionCard(
                    Modifier.clip(RoundedCornerShape(20.dp))
                        .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)) else Modifier)
                        .clickable { vm.update { it.copy(planId = plan.id) } }
                ) {
                    Row {
                        Text(plan.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(if (plan.pricePen == 0.0) "Gratis" else "S/ ${"%.2f".format(plan.pricePen)}/mes")
                    }
                    plan.features.forEach { feature ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp).size(18.dp))
                            Text(feature, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (f.planId == plan.id) Text("Seleccionado ✓", color = MaterialTheme.colorScheme.primary)
                }
            }
            if (f.planId != null) {
                Text("Datos de pago", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NzTextField(f.firstName, { v -> vm.update { it.copy(firstName = v) } }, "Nombre", Modifier.weight(1f))
                    NzTextField(f.lastName, { v -> vm.update { it.copy(lastName = v) } }, "Apellido", Modifier.weight(1f))
                }
                NzTextField(f.email, { v -> vm.update { it.copy(email = v) } }, "Correo", keyboardType = KeyboardType.Email)
                NzTextField(f.card, { v -> vm.update { it.copy(card = v.filter(Char::isDigit).take(19)) } }, "Número de tarjeta", keyboardType = KeyboardType.Number)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NzTextField(f.expiration, { v -> vm.update { it.copy(expiration = formatExpiry(v)) } }, "MM/AA", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    NzTextField(f.cvv, { v -> vm.update { it.copy(cvv = v.filter(Char::isDigit).take(4)) } }, "CVV", Modifier.weight(1f), keyboardType = KeyboardType.NumberPassword)
                }
                f.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                PrimaryButton("Suscribirme", vm::pay, loading = f.paying)
                Text("Pago de demostración del proyecto. NeuroZen solo guarda los últimos 4 dígitos y el vencimiento; nunca se guardan datos de tarjeta en el teléfono.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatExpiry(input: String): String {
    val digits = input.filter(Char::isDigit).take(4)
    return if (digits.length > 2) digits.substring(0, 2) + "/" + digits.substring(2) else digits
}
