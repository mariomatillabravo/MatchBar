package com.matchbar.app.ui.screens.bar

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchbar.app.data.api.MatchBarApi
import com.matchbar.app.data.model.Bar
import com.matchbar.app.data.model.BarUpsertRequest
import com.matchbar.app.ui.common.Appear
import com.matchbar.app.ui.common.GradientButton
import com.matchbar.app.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class MyBarUiState(
    val bar: Bar? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val uploadingLicense: Boolean = false,
    val licenseFilename: String? = null,
    val error: String? = null,
    val info: String? = null
)

class MyBarViewModel(private val api: MatchBarApi) : ViewModel() {
    private val _state = MutableStateFlow(MyBarUiState())
    val state: StateFlow<MyBarUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching { api.myBar() }
            .onSuccess { bar ->
                _state.update { it.copy(loading = false, bar = bar, licenseFilename = bar.licenseDocFilename) }
            }
            .onFailure { e ->
                if (e is retrofit2.HttpException && e.code() == 404) {
                    _state.update { it.copy(loading = false, bar = null) }
                } else {
                    _state.update { it.copy(loading = false, error = e.userMessage()) }
                }
            }
    }

    fun save(req: BarUpsertRequest) = viewModelScope.launch {
        _state.update { it.copy(saving = true, error = null, info = null) }
        runCatching { api.upsertMyBar(req) }
            .onSuccess { _state.update { st -> st.copy(saving = false, bar = it,
                info = "Cambios guardados. Estado: ${it.status}") } }
            .onFailure { e -> _state.update { it.copy(saving = false, error = e.userMessage()) } }
    }

    fun uploadLicense(context: Context, uri: Uri) = viewModelScope.launch {
        _state.update { it.copy(uploadingLicense = true, error = null, info = null) }
        runCatching {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri) ?: "application/pdf"
            val filename = queryDisplayName(context, uri) ?: "licencia.pdf"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("No se pudo leer el fichero")
            val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", filename, body)
            api.uploadLicense(part)
        }.onSuccess { res ->
            _state.update { it.copy(uploadingLicense = false,
                licenseFilename = res.filename,
                info = "Licencia subida correctamente") }
        }.onFailure { e ->
            _state.update { it.copy(uploadingLicense = false, error = e.userMessage()) }
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
    }

    fun clearMessages() = _state.update { it.copy(info = null, error = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBarScreen(
    vmFactory: androidx.lifecycle.ViewModelProvider.Factory,
    onOpenSettings: () -> Unit
) {
    val vm: MyBarViewModel = viewModel(factory = vmFactory)
    val state by vm.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.info, state.error) {
        state.info?.let { snackbarHost.showSnackbar(it); vm.clearMessages() }
        state.error?.let { snackbarHost.showSnackbar(it); vm.clearMessages() }
    }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("") }

    LaunchedEffect(state.bar) {
        state.bar?.let {
            name = it.name
            description = it.description.orEmpty()
            address = it.address
            ownerPhone = it.ownerPhone.orEmpty()
        }
    }

    val pickPdf = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { vm.uploadLicense(context, it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Mi bar") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, "Ajustes")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Appear {
                state.bar?.let { StatusPill(it.status) }
                    ?: Text(
                        "Aún no has creado tu ficha de bar. Rellena los datos para aparecer en el mapa.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
            }

            Spacer(Modifier.height(16.dp))

            Appear(indexForStagger = 1) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("DATOS DEL BAR", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(name, { name = it },
                            label = { Text("Nombre del bar") },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(description, { description = it },
                            label = { Text("Descripción") },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(address, { address = it },
                            label = { Text("Dirección") },
                            placeholder = { Text("Ej: Calle Bravo Murillo 34, Madrid") },
                            supportingText = {
                                Text("Indica calle, número y ciudad. Ubicaremos tu bar automáticamente.")
                            },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(ownerPhone, { ownerPhone = it },
                            label = { Text("Teléfono de contacto") },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Phone))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Appear(indexForStagger = 2) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("LICENCIA DE ACTIVIDAD (PDF)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        state.licenseFilename?.let {
                            Text("Adjuntada: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                        }
                        OutlinedButton(
                            onClick = { pickPdf.launch("application/pdf") },
                            enabled = state.bar != null && !state.uploadingLicense,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.uploadingLicense) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(if (state.licenseFilename != null) "Reemplazar PDF" else "Adjuntar PDF")
                            }
                        }
                        if (state.bar == null) {
                            Spacer(Modifier.height(6.dp))
                            Text("Guarda primero la ficha del bar para poder subir la licencia.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = if (state.bar == null) "Crear ficha" else "Guardar cambios",
                onClick = {
                    vm.save(BarUpsertRequest(
                        name = name.trim(),
                        description = description.ifBlank { null },
                        address = address.trim(),
                        ownerPhone = ownerPhone.ifBlank { null }
                    ))
                },
                enabled = !state.saving && name.isNotBlank() && address.isNotBlank(),
                loading = state.saving
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val (container, content, label) = when (status) {
        "APPROVED" -> Triple(MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer, "Aprobado")
        "REJECTED" -> Triple(MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer, "Rechazado")
        else -> Triple(MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer, "Pendiente de revisión")
    }
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(container)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text("Estado: $label", color = content,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}
