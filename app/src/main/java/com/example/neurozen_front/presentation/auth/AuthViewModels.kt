package com.example.neurozen_front.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neurozen_front.domain.logic.Validators
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
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val specialty: String = "",
    val licenseNumber: String = "",
    val acceptedTerms: Boolean = false,
    val acceptedHealthData: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RegisterViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun update(transform: (RegisterUiState) -> RegisterUiState) = _state.update { transform(it).copy(error = null) }

    fun validate(s: RegisterUiState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (!Validators.isValidFullName(s.fullName)) errors["fullName"] = "Escribe tu nombre y apellido"
        if (!Validators.isValidEmail(s.email)) errors["email"] = "Correo inválido"
        Validators.passwordError(s.password)?.let { errors["password"] = it }
        if (s.confirmPassword != s.password) errors["confirm"] = "Las contraseñas no coinciden"
        if (s.role == Role.PSYCHOLOGIST) {
            if (s.specialty.isBlank()) errors["specialty"] = "Indica tu especialidad"
            if (!Validators.isValidLicense(s.licenseNumber)) errors["license"] = "N.° de colegiatura CPsP inválido (solo números)"
        }
        if (!s.acceptedTerms) errors["terms"] = "Debes aceptar los términos"
        if (s.role == Role.USER && !s.acceptedHealthData) errors["health"] = "Necesitamos tu consentimiento para tratar datos de salud"
        return errors
    }

    fun submit() {
        val s = _state.value
        val errors = validate(s)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        _state.update { it.copy(loading = true, errors = emptyMap()) }
        viewModelScope.launch {
            val result = auth.signUp(
                RegisterData(s.fullName, s.email, s.password, s.role, s.acceptedTerms, s.specialty, s.licenseNumber)
            )
            _state.update {
                if (result is Outcome.Failure) it.copy(loading = false, error = result.error.message) else it.copy(loading = false)
            }
        }
    }
}

data class PasswordUiState(
    val email: String = "",
    val token: String = "",
    val password: String = "",
    val confirm: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

@HiltViewModel
class PasswordViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(PasswordUiState())
    val state: StateFlow<PasswordUiState> = _state.asStateFlow()

    fun update(transform: (PasswordUiState) -> PasswordUiState) = _state.update { transform(it).copy(error = null) }

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
        val error = when {
            s.token.isBlank() -> "Falta el código del enlace"
            Validators.passwordError(s.password) != null -> Validators.passwordError(s.password)
            s.password != s.confirm -> "Las contraseñas no coinciden"
            else -> null
        }
        if (error != null) { _state.update { it.copy(error = error) }; return }
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val result = auth.resetPassword(s.token, s.password)
            _state.update {
                when (result) {
                    is Outcome.Success -> it.copy(loading = false, success = result.data, password = "", confirm = "")
                    is Outcome.Failure -> it.copy(loading = false, error = result.error.message)
                }
            }
        }
    }
}
