package com.example.neurozen_front.presentation.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.example.neurozen_front.BuildConfig
import com.example.neurozen_front.R
import com.example.neurozen_front.domain.logic.ResetLink
import androidx.lifecycle.viewModelScope
import com.example.neurozen_front.domain.logic.Catalogs
import com.example.neurozen_front.domain.logic.Validators
import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.RegisterData
import com.example.neurozen_front.domain.model.Role
import com.example.neurozen_front.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmail(value: String) = _state.update { it.copy(email = value, emailError = null, error = null) }
    fun onPassword(value: String) = _state.update { it.copy(password = value, passwordError = null, error = null) }

    /** On success the session is saved and AppRoot navigates by role. */
    fun submit() {
        val s = _state.value
        val emailError = if (!Validators.isValidEmail(s.email)) "Ingresa un correo válido" else null
        val passwordError = if (s.password.isBlank()) "Ingresa tu contraseña" else null
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = auth.signIn(s.email, s.password)
            _state.update {
                when (result) {
                    is Outcome.Success -> it.copy(loading = false, password = "")
                    is Outcome.Failure -> it.copy(loading = false, error = result.error.message)
                }
            }
        }
    }
}

data class RegisterUiState(
    val role: Role = Role.USER,
    val fullName: String = "",
    val dni: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val specialty: String = "",
    val licenseNumber: String = "",
    // Consents are never pre-checked (Ley 29733)
    val acceptedTerms: Boolean = false,
    val acceptedPrivacyPolicy: Boolean = false,
    val acceptedHealthData: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val loading: Boolean = false,
    val error: String? = null
)

/**
 * Sign-up with the backend contract: DNI (8 digits) for every role, confirmPassword and two separate consents
 * (Terms → acceptedTerms, Privacy Policy → acceptedPrivacyPolicy). The DNI and the passwords live only in this
 * state (memory); they are never saved in DataStore nor logged.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    /** Editing a field clears the server error of that field. */
    fun update(transform: (RegisterUiState) -> RegisterUiState) = _state.update { old ->
        val new = transform(old)
        val changed = FIELDS.filter { (key, read) -> read(old) != read(new) }.map { it.first }.toSet()
        new.copy(error = null, errors = new.errors - changed)
    }

    fun validate(s: RegisterUiState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        Validators.fullNameError(s.fullName)?.let { errors["fullName"] = it }
        Validators.dniError(s.dni)?.let { errors["dni"] = it }
        if (!Validators.isValidEmail(s.email)) errors["email"] = "Ingresa un correo válido"
        Validators.passwordError(s.password)?.let { errors["password"] = it }
        if (s.confirmPassword != s.password) errors["confirm"] = "Las contraseñas no coinciden"
        if (s.role == Role.PSYCHOLOGIST) {
            if (Catalogs.SPECIALTIES.none { it.key == s.specialty }) errors["specialty"] = "Selecciona una especialidad válida"
            if (!Validators.isValidLicense(s.licenseNumber)) errors["license"] = "El número de colegiatura debe tener entre 3 y 6 dígitos"
        }
        if (!s.acceptedTerms) errors["terms"] = "Debes aceptar los Términos y Condiciones"
        if (!s.acceptedPrivacyPolicy) errors["privacy"] = "Debes aceptar la Política de Privacidad"
        if (s.role == Role.USER && !s.acceptedHealthData) errors["health"] = "Necesitamos tu consentimiento para tratar datos de salud"
        return errors
    }

    fun submit() {
        val s = _state.value
        if (s.loading) return
        val errors = validate(s)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        _state.update { it.copy(loading = true, errors = emptyMap()) }
        viewModelScope.launch {
            val result = auth.signUp(
                RegisterData(
                    fullName = s.fullName, email = s.email, password = s.password, role = s.role,
                    acceptedTerms = s.acceptedTerms, specialty = s.specialty, licenseNumber = s.licenseNumber,
                    dni = s.dni, confirmPassword = s.confirmPassword, acceptedPrivacyPolicy = s.acceptedPrivacyPolicy
                )
            )
            _state.update {
                if (result is Outcome.Failure) {
                    val (fieldErrors, general) = serverErrors(result.error)
                    it.copy(loading = false, errors = fieldErrors, error = general)
                } else it.copy(loading = false, password = "", confirmPassword = "")
            }
        }
    }

    companion object {
        /** Form keys and how to read them (used to clear a server error when its field changes). */
        private val FIELDS: List<Pair<String, (RegisterUiState) -> Any>> = listOf(
            "fullName" to { s -> s.fullName }, "dni" to { s -> s.dni }, "email" to { s -> s.email },
            "password" to { s -> s.password }, "confirm" to { s -> s.confirmPassword }, "specialty" to { s -> s.specialty },
            "license" to { s -> s.licenseNumber }, "terms" to { s -> s.acceptedTerms },
            "privacy" to { s -> s.acceptedPrivacyPolicy }, "health" to { s -> s.acceptedHealthData }
        )

        /** Backend field → form key. */
        private val SERVER_FIELDS = mapOf(
            "fullname" to "fullName", "firstname" to "fullName", "lastname" to "fullName", "dni" to "dni",
            "email" to "email", "username" to "email", "password" to "password", "confirmpassword" to "confirm",
            "specialty" to "specialty", "licensenumber" to "license", "acceptedterms" to "terms",
            "acceptedprivacypolicy" to "privacy"
        )

        /**
         * 400 → each message under its field; 409 (email, DNI, license) → under its field; 429 and the rest → general
         * message. Returns (field errors, general message).
         */
        fun serverErrors(error: AppError): Pair<Map<String, String>, String?> = when (error) {
            is AppError.Validation -> {
                val fields = error.fieldErrors.mapNotNull { (key, messages) ->
                    val target = SERVER_FIELDS[key.lowercase()] ?: return@mapNotNull null
                    messages.firstOrNull()?.let { target to it }
                }.toMap()
                fields to (if (fields.isEmpty()) error.message else "Revisa los campos marcados.")
            }
            is AppError.Conflict -> {
                val message = error.message
                val field = when {
                    message.contains("DNI", ignoreCase = true) -> "dni"
                    message.contains("colegiatura", ignoreCase = true) -> "license"
                    message.contains("correo", ignoreCase = true) -> "email"
                    else -> null
                }
                if (field != null) mapOf(field to message) to null else emptyMap<String, String>() to message
            }
            else -> emptyMap<String, String>() to error.message
        }
    }
}

data class PasswordUiState(
    val email: String = "",
    /** Token, or the whole link pasted by the user (the token is extracted and validated before sending). */
    val token: String = "",
    val password: String = "",
    val confirm: String = "",
    val loading: Boolean = false,
    /** Backend message or password rule text. */
    val error: String? = null,
    /** Local message from strings.xml (link missing/invalid, passwords do not match). */
    @param:StringRes val errorRes: Int? = null,
    val success: String? = null
)

@HiltViewModel
class PasswordViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(PasswordUiState())
    val state: StateFlow<PasswordUiState> = _state.asStateFlow()

    fun update(transform: (PasswordUiState) -> PasswordUiState) = _state.update { transform(it).copy(error = null, errorRes = null) }

    fun requestLink() {
        val email = _state.value.email
        if (!Validators.isValidEmail(email)) { _state.update { it.copy(error = "Ingresa un correo válido") }; return }
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val result = auth.forgotPassword(email)
            _state.update {
                when (result) {
                    is Outcome.Success -> it.copy(loading = false, success = result.data)
                    is Outcome.Failure -> it.copy(loading = false, error = result.error.message)
                }
            }
        }
    }

    fun reset() {
        val s = _state.value
        if (s.loading) return
        val token = ResetLink.tokenFromUserInput(s.token, BuildConfig.RESET_LINK_HOST)
        val errorRes = when {
            s.token.isBlank() -> R.string.reset_link_missing
            token == null -> R.string.reset_link_invalid
            Validators.passwordError(s.password) == null && s.password != s.confirm -> R.string.reset_mismatch
            else -> null
        }
        val passwordError = if (errorRes == null) Validators.passwordError(s.password) else null
        if (errorRes != null || passwordError != null || token == null) {
            _state.update { it.copy(errorRes = errorRes, error = passwordError) }
            return
        }
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val result = auth.resetPassword(token, s.password)
            _state.update {
                when (result) {
                    is Outcome.Success -> it.copy(loading = false, success = result.data, password = "", confirm = "", token = "")
                    is Outcome.Failure -> it.copy(loading = false, error = result.error.message)
                }
            }
        }
    }
}
