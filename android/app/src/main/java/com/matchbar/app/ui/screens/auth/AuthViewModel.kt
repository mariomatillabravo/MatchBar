package com.matchbar.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matchbar.app.data.api.MatchBarApi
import com.matchbar.app.data.local.SessionStore
import com.matchbar.app.data.model.LoginRequest
import com.matchbar.app.data.model.RegisterRequest
import com.matchbar.app.data.model.Role
import com.matchbar.app.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val role: Role = Role.USER,
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class AuthViewModel(
    private val api: MatchBarApi,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onEmail(v: String) = _state.update { it.copy(email = v, error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onName(v: String) = _state.update { it.copy(name = v, error = null) }
    fun onRole(r: Role) = _state.update { it.copy(role = r) }

    fun login() = viewModelScope.launch {
        val s = _state.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(error = "Rellena email y contraseña") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null) }
        runCatching { api.login(LoginRequest(s.email.trim(), s.password)) }
            .onSuccess { res ->
                sessionStore.save(res.token, res.userId, res.email, res.name, res.role)
                _state.update { it.copy(loading = false, success = true) }
            }
            .onFailure { e ->
                _state.update { it.copy(loading = false, error = e.userMessage()) }
            }
    }

    fun register() = viewModelScope.launch {
        val s = _state.value
        if (s.email.isBlank() || s.password.length < 8 || s.name.isBlank()) {
            _state.update { it.copy(error = "Email, nombre y contraseña (mín. 8) son obligatorios") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null) }
        runCatching { api.register(RegisterRequest(s.email.trim(), s.password, s.name.trim(), s.role)) }
            .onSuccess { res ->
                sessionStore.save(res.token, res.userId, res.email, res.name, res.role)
                _state.update { it.copy(loading = false, success = true) }
            }
            .onFailure { e ->
                _state.update { it.copy(loading = false, error = e.userMessage()) }
            }
    }
}
