package com.matchbar.app.ui.screens.bar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchbar.app.data.api.MatchBarApi
import com.matchbar.app.data.model.Bar
import com.matchbar.app.data.model.BarUpsertRequest
import com.matchbar.app.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyBarUiState(
    val bar: Bar? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
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
            .onSuccess { _state.update { st -> st.copy(loading = false, bar = it) } }
            .onFailure { e ->
                // Si es 404, no hay bar todavía. Eso no es error, mostraremos el formulario vacío.
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

    fun clearMessages() = _state.update { it.copy(info = null, error = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBarScreen(
    vmFactory: androidx.lifecycle.ViewModelProvider.Factory,
    onLogout: () -> Unit
) {
    val vm: MyBarViewModel = viewModel(factory = vmFactory)
    val state by vm.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.info, state.error) {
        state.info?.let { snackbarHost.showSnackbar(it); vm.clearMessages() }
        state.error?.let { snackbarHost.showSnackbar(it); vm.clearMessages() }
    }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("40.4168") }
    var lng by remember { mutableStateOf("-3.7038") }

    LaunchedEffect(state.bar) {
        state.bar?.let {
            name = it.name
            description = it.description.orEmpty()
            address = it.address
            lat = it.latitude.toString()
            lng = it.longitude.toString()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Mi bar") },
                actions = { TextButton(onClick = onLogout) { Text("Salir") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            state.bar?.let {
                AssistChip(onClick = {}, label = { Text("Estado: ${it.status}") })
                Spacer(Modifier.height(12.dp))
            } ?: Text("Aún no has creado tu ficha de bar. Rellena los datos abajo.",
                style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(name, { name = it },
                label = { Text("Nombre del bar") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(description, { description = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(address, { address = it },
                label = { Text("Dirección") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            Row {
                OutlinedTextField(lat, { lat = it },
                    label = { Text("Latitud") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal),
                    singleLine = true)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(lng, { lng = it },
                    label = { Text("Longitud") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal),
                    singleLine = true)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val latNum = lat.toDoubleOrNull() ?: 0.0
                    val lngNum = lng.toDoubleOrNull() ?: 0.0
                    vm.save(BarUpsertRequest(
                        name = name.trim(),
                        description = description.ifBlank { null },
                        address = address.trim(),
                        latitude = latNum,
                        longitude = lngNum
                    ))
                },
                enabled = !state.saving && name.isNotBlank() && address.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.saving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else Text(if (state.bar == null) "Crear ficha" else "Guardar cambios")
            }
        }
    }
}
