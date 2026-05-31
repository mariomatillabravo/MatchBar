package com.matchbar.app.ui.screens.auth

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matchbar.app.data.api.MatchBarApi
import com.matchbar.app.data.local.SessionStore
import com.matchbar.app.data.model.BarUpsertRequest
import com.matchbar.app.data.model.LoginRequest
import com.matchbar.app.data.model.RegisterRequest
import com.matchbar.app.data.model.Role
import com.matchbar.app.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val role: Role = Role.USER,
    // Datos del local (solo cuando role == BAR)
    val barName: String = "",
    val barDescription: String = "",
    val barAddress: String = "",
    val barPhone: String = "",
    val photoUris: List<Uri> = emptyList(),
    val menuUris: List<Uri> = emptyList(),
    val licenseUri: Uri? = null,
    val licenseName: String? = null,
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

    fun onBarName(v: String) = _state.update { it.copy(barName = v, error = null) }
    fun onBarDescription(v: String) = _state.update { it.copy(barDescription = v, error = null) }
    fun onBarAddress(v: String) = _state.update { it.copy(barAddress = v, error = null) }
    fun onBarPhone(v: String) = _state.update { it.copy(barPhone = v, error = null) }

    fun addPhotos(uris: List<Uri>) =
        _state.update { it.copy(photoUris = it.photoUris + uris, error = null) }
    fun removePhoto(uri: Uri) =
        _state.update { it.copy(photoUris = it.photoUris - uri) }

    fun addMenus(uris: List<Uri>) =
        _state.update { it.copy(menuUris = it.menuUris + uris, error = null) }
    fun removeMenu(uri: Uri) =
        _state.update { it.copy(menuUris = it.menuUris - uri) }

    fun setLicense(context: Context, uri: Uri) =
        _state.update { it.copy(licenseUri = uri, licenseName = queryDisplayName(context, uri) ?: "licencia.pdf", error = null) }
    fun clearLicense() = _state.update { it.copy(licenseUri = null, licenseName = null) }

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

    fun register(context: Context) = viewModelScope.launch {
        val s = _state.value
        if (s.email.isBlank() || s.password.length < 8 || s.name.isBlank()) {
            _state.update { it.copy(error = "Email, nombre y contraseña (mín. 8) son obligatorios") }
            return@launch
        }
        if (s.role == Role.BAR && (s.barName.isBlank() || s.barAddress.isBlank())) {
            _state.update { it.copy(error = "Indica el nombre y la dirección del local") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            val res = api.register(RegisterRequest(s.email.trim(), s.password, s.name.trim(), s.role))
            if (s.role == Role.BAR) {
                // Guardamos solo el token para poder autenticar las subidas sin
                // que la navegación cambie de pantalla todavía (eso cancelaría
                // este flujo). La sesión completa se guarda al final.
                sessionStore.saveToken(res.token)
                api.upsertMyBar(
                    BarUpsertRequest(
                        name = s.barName.trim(),
                        description = s.barDescription.trim().ifBlank { null },
                        address = s.barAddress.trim(),
                        ownerPhone = s.barPhone.ifBlank { null }
                    )
                )
                s.photoUris.forEach { api.uploadBarPhoto(partFromUri(context, it, "foto.jpg")) }
                s.menuUris.forEach { api.uploadBarMenu(partFromUri(context, it, "carta.jpg")) }
                s.licenseUri?.let { api.uploadLicense(partFromUri(context, it, "licencia.pdf")) }
            }
            res
        }.onSuccess { res ->
            sessionStore.save(res.token, res.userId, res.email, res.name, res.role)
            _state.update { it.copy(loading = false, success = true) }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, error = e.userMessage()) }
        }
    }

    private fun partFromUri(context: Context, uri: Uri, fallbackName: String): MultipartBody.Part {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val filename = queryDisplayName(context, uri) ?: fallbackName
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("No se pudo leer el fichero")
        val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("file", filename, body)
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
    }
}
