package com.example.neurozen_front.presentation.auth

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.neurozen_front.R
import com.example.neurozen_front.domain.model.Role as UserRole
import com.example.neurozen_front.domain.logic.Validators
import com.example.neurozen_front.presentation.components.DigitsField
import com.example.neurozen_front.presentation.components.ErrorBanner
import com.example.neurozen_front.presentation.components.SpecialtyField
import com.example.neurozen_front.presentation.components.NzTextField
import com.example.neurozen_front.presentation.components.PasswordField
import com.example.neurozen_front.presentation.components.PasswordRequirements
import androidx.compose.ui.res.stringResource
import com.example.neurozen_front.presentation.components.PrimaryButton
import com.example.neurozen_front.presentation.components.SuccessBanner
import com.example.neurozen_front.presentation.theme.LocalNzDarkTheme
import com.example.neurozen_front.presentation.theme.NzColors

@Composable
fun AuthNavHost(notice: String?, onNoticeShown: () -> Unit) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(onStart = { nav.navigate("register") }, onLogin = { nav.navigate("login") })
        }
        composable("login") {
            val vm: LoginViewModel = hiltViewModel()
            val state by vm.state.collectAsState()
            LoginScreen(
                state = state, notice = notice, onNoticeShown = onNoticeShown,
                onEmail = vm::onEmail, onPassword = vm::onPassword, onSubmit = vm::submit,
                onForgot = { nav.navigate("forgot") }, onRegister = { nav.navigate("register") { popUpTo("welcome") } },
                onBack = { nav.popBackStack() }
            )
        }
        composable("register") { RegisterScreen(onLogin = { nav.navigate("login") { popUpTo("welcome") } }) }
        composable("forgot") { ForgotPasswordScreen(onBack = { nav.popBackStack() }, onHaveCode = { nav.navigate("reset") }) }
        composable("reset") { ResetPasswordScreen(initialToken = "", onDone = { nav.popBackStack("login", inclusive = false) }) }
    }
}

// ---------- Original onboarding design ----------

@Composable
fun WelcomeScreen(onStart: () -> Unit, onLogin: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                if (LocalNzDarkTheme.current) listOf(NzColors.Night, NzColors.NightSurface, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                else listOf(NzColors.OnboardingTop, NzColors.OnboardingMiddle, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            )
        )
    ) {
        // Soft background circle
        Surface(modifier = Modifier.offset(x = (-50).dp, y = (-50).dp).size(200.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.3f)) {}
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(40.dp))
            Surface(modifier = Modifier.size(160.dp), shape = RoundedCornerShape(40.dp), color = Color.White, shadowElevation = 12.dp) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.logo_neurozen),
                        contentDescription = "Logo NeuroZen",
                        modifier = Modifier.size(130.dp).clip(RoundedCornerShape(30.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Neurozen", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Tu refugio digital para encontrar equilibrio y serenidad en cada respiración.",
                    style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) { Text("Comenzar ahora", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                TextButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
                    Text("Ya tengo una cuenta", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ---------- Original login layout: gradient, icon badge, white card ----------

@Composable
private fun AuthLayout(
    emoji: String,
    title: String,
    subtitle: String,
    snackbar: SnackbarHostState? = null,
    header: @Composable ColumnScope.() -> Unit = {},
    footer: @Composable ColumnScope.() -> Unit = {},
    card: @Composable ColumnScope.() -> Unit
) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(if (LocalNzDarkTheme.current) listOf(NzColors.Night, NzColors.NightSurface) else listOf(NzColors.LoginTop, NzColors.LoginBottom)))) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(Modifier.size(80.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primary, shadowElevation = 8.dp) {
                Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = 32.sp) }
            }
            Spacer(Modifier.height(24.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            header()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = card)
            }
            Spacer(Modifier.height(20.dp))
            footer()
            Spacer(Modifier.height(24.dp))
            Text("Seguro • Privado • Neurozen", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
        snackbar?.let { SnackbarHost(it, Modifier.align(Alignment.BottomCenter).systemBarsPadding()) }
    }
}

/** Stateless so it can be tested with Compose UI tests. */
@Composable
fun LoginScreen(
    state: LoginUiState,
    notice: String?,
    onNoticeShown: () -> Unit,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSubmit: () -> Unit,
    onForgot: () -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(notice) { notice?.let { snackbar.showSnackbar(it); onNoticeShown() } }
    AuthLayout(
        emoji = "🔐", title = "Bienvenido de vuelta", subtitle = "Ingresa tus datos de acceso.", snackbar = snackbar,
        footer = { TextButton(onClick = onBack) { Text("Volver al inicio", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium) } }
    ) {
        NzTextField(state.email, onEmail, "Correo electrónico", Modifier.testTag("login_email"), state.emailError, KeyboardType.Email)
        PasswordField(state.password, onPassword, error = state.passwordError)
        state.error?.let { ErrorBanner(it, Modifier.testTag("login_error")) }
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Iniciar Sesión", onSubmit, Modifier.testTag("login_submit"), loading = state.loading)
        TextButton(onClick = onForgot, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("¿Olvidaste tu contraseña?", color = MaterialTheme.colorScheme.primary)
        }
        TextButton(onClick = onRegister, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("¿No tienes cuenta? Regístrate", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onLogin: () -> Unit, vm: RegisterViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    var showTerms by rememberSaveable { mutableStateOf(false) }
    val isPsychologist = s.role == UserRole.PSYCHOLOGIST
    AuthLayout(
        emoji = if (isPsychologist) "🩺" else "🌿",
        title = "Crea tu cuenta",
        subtitle = "Únete a la plataforma de NeuroZen",
        header = {
            // Original role selector (Paciente / Psicólogo)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !isPsychologist, onClick = { vm.update { it.copy(role = UserRole.USER) } },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Paciente") }
                SegmentedButton(
                    selected = isPsychologist, onClick = { vm.update { it.copy(role = UserRole.PSYCHOLOGIST) } },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Psicólogo") }
            }
            Spacer(Modifier.height(24.dp))
        }
    ) {
        FormSection("Tus datos")
        NzTextField(s.fullName, { v -> vm.update { it.copy(fullName = v.take(Validators.FULL_NAME_MAX_LENGTH)) } }, "Nombre completo",
            Modifier.testTag("register_name"), error = s.errors["fullName"],
            supporting = if (isPsychologist) "Debe coincidir con tu DNI y tu registro en el CPsP." else null)
        // DNI: required for every role; only the format is validated (it is not checked against RENIEC)
        DigitsField(s.dni, { v -> vm.update { it.copy(dni = v) } }, "DNI", Validators.DNI_LENGTH, Modifier.testTag("register_dni"),
            error = s.errors["dni"], supporting = "Solo números: exactamente 8 dígitos, sin espacios.")
        NzTextField(s.email, { v -> vm.update { it.copy(email = v) } }, "Correo electrónico", Modifier.testTag("register_email"),
            error = s.errors["email"], keyboardType = KeyboardType.Email)
        if (isPsychologist) {
            FormSection("Datos profesionales")
            SpecialtyField(s.specialty, { v -> vm.update { it.copy(specialty = v) } }, error = s.errors["specialty"])
            DigitsField(
                s.licenseNumber, { v -> vm.update { it.copy(licenseNumber = v) } }, "N.° de colegiatura (CPsP)", Validators.LICENSE_MAX_LENGTH,
                Modifier.testTag("register_license"), error = s.errors["license"],
                supporting = "Un administrador lo validará en el portal del CPsP antes de que recibas citas."
            )
        }
        FormSection("Acceso")
        PasswordField(s.password, { v -> vm.update { it.copy(password = v) } }, error = s.errors["password"])
        PasswordRequirements(s.password)
        PasswordField(s.confirmPassword, { v -> vm.update { it.copy(confirmPassword = v) } }, "Confirmar contraseña", s.errors["confirm"])
        // Two separate consents, never pre-checked (Ley 29733). The existing legal summary is reused, not rewritten.
        FormSection("Consentimientos")
        CheckRow(
            checked = s.acceptedTerms, error = s.errors["terms"], tag = "register_terms",
            text = "Acepto los Términos y Condiciones",
            onChange = { v -> vm.update { it.copy(acceptedTerms = v) } }
        )
        CheckRow(
            checked = s.acceptedPrivacyPolicy, error = s.errors["privacy"], tag = "register_privacy",
            text = "Acepto la Política de Privacidad y el tratamiento de mis datos personales (Ley 29733)",
            onChange = { v -> vm.update { it.copy(acceptedPrivacyPolicy = v) } }
        )
        TextButton(onClick = { showTerms = !showTerms }) {
            Text(if (showTerms) "Ocultar términos y política" else "Leer términos y política de privacidad", color = MaterialTheme.colorScheme.primary)
        }
        if (showTerms) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                Text(TERMS_SUMMARY, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
            }
        }
        if (!isPsychologist) {
            CheckRow(
                checked = s.acceptedHealthData, error = s.errors["health"], tag = "register_health",
                text = "Autorizo el tratamiento de mis datos de salud (registros de estrés, tests) para fines de bienestar. Puedo eliminar mi cuenta y mis datos en cualquier momento.",
                onChange = { v -> vm.update { it.copy(acceptedHealthData = v) } }
            )
        }
        s.error?.let { ErrorBanner(it, Modifier.testTag("register_error")) }
        Spacer(Modifier.height(8.dp))
        PrimaryButton(if (isPsychologist) "Registrarme como Psicólogo" else "Registrarse", vm::submit, Modifier.testTag("register_submit"), loading = s.loading)
        TextButton(onClick = onLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("¿Ya tienes cuenta? Inicia sesión", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private const val TERMS_SUMMARY =
    "NeuroZen es una herramienta de bienestar y no reemplaza la atención médica ni psicológica de emergencia. " +
        "Tus datos se usan solo para mostrarte tu progreso y, si tú lo decides, compartir un informe con tu psicólogo. " +
        "Puedes eliminar tu cuenta y tus datos personales desde Perfil > Eliminar cuenta (Ley 29733)."

/** Small caps title that groups the fields of the form. */
@Composable
private fun FormSection(title: String) {
    Text(
        title.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp).semantics { heading() }
    )
}

@Composable
private fun CheckRow(checked: Boolean, text: String, error: String?, tag: String? = null, onChange: (Boolean) -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().then(if (tag != null) Modifier.testTag(tag) else Modifier)
                .toggleable(value = checked, onValueChange = onChange, role = Role.Checkbox),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = null)
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, onHaveCode: () -> Unit, vm: PasswordViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    AuthLayout(
        emoji = "✉️", title = "Recuperar contraseña", subtitle = "Te enviaremos un enlace a tu correo.",
        footer = { TextButton(onClick = onBack) { Text("Volver", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium) } }
    ) {
        NzTextField(s.email, { v -> vm.update { it.copy(email = v) } }, "Correo electrónico", keyboardType = KeyboardType.Email)
        s.error?.let { ErrorBanner(it) }
        s.success?.let { SuccessBanner(it) }
        PrimaryButton("Enviar enlace", vm::requestLink, loading = s.loading)
        TextButton(onClick = onHaveCode, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.forgot_have_link), color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ResetPasswordScreen(initialToken: String, onDone: () -> Unit, invalidLink: Boolean = false, vm: PasswordViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    LaunchedEffect(initialToken) { if (initialToken.isNotBlank()) vm.update { it.copy(token = initialToken) } }
    val fromLink = initialToken.isNotBlank()
    AuthLayout(
        emoji = "🔑", title = stringResource(R.string.reset_title), subtitle = stringResource(R.string.reset_subtitle),
        footer = { TextButton(onClick = onDone) { Text(stringResource(R.string.reset_cancel), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium) } }
    ) {
        when {
            fromLink -> SuccessBanner(stringResource(R.string.reset_link_loaded))
            invalidLink -> ErrorBanner(stringResource(R.string.reset_link_invalid))
            else -> Unit
        }
        if (!fromLink) {
            NzTextField(
                s.token, { v -> vm.update { it.copy(token = v.take(2048)) } }, stringResource(R.string.reset_link_field),
                Modifier.testTag("reset_token"), supporting = stringResource(R.string.reset_link_field_hint), keyboardType = KeyboardType.Uri
            )
        }
        PasswordField(s.password, { v -> vm.update { it.copy(password = v) } }, stringResource(R.string.reset_new_password))
        PasswordRequirements(s.password)
        PasswordField(s.confirm, { v -> vm.update { it.copy(confirm = v) } }, stringResource(R.string.reset_confirm_password))
        s.errorRes?.let { ErrorBanner(stringResource(it)) }
        s.error?.let { ErrorBanner(it) }
        val success = s.success
        if (success != null) {
            SuccessBanner(success)
            PrimaryButton(stringResource(R.string.reset_go_login), onDone)
        } else {
            PrimaryButton(stringResource(R.string.reset_save), vm::reset, loading = s.loading)
        }
    }
}
