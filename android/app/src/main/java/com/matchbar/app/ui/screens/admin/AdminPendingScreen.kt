package com.matchbar.app.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchbar.app.data.api.MatchBarApi
import com.matchbar.app.data.model.Bar
import com.matchbar.app.ui.common.EmptyBox
import com.matchbar.app.ui.common.ErrorBox
import com.matchbar.app.ui.common.LoadingBox
import com.matchbar.app.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUiState(
    val pending: List<Bar> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class AdminViewModel(private val api: MatchBarApi) : ViewModel() {
    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching { api.pendingBars() }
            .onSuccess { _state.update { st -> st.copy(loading = false, pending = it) } }
            .onFailure { e -> _state.update { it.copy(loading = false, error = e.userMessage()) } }
    }

    fun approve(id: Long) = viewModelScope.launch {
        runCatching { api.approveBar(id) }.onSuccess { load() }
    }

    fun reject(id: Long) = viewModelScope.launch {
        runCatching { api.rejectBar(id) }.onSuccess { load() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPendingScreen(
    vmFactory: androidx.lifecycle.ViewModelProvider.Factory,
    onLogout: () -> Unit
) {
    val vm: AdminViewModel = viewModel(factory = vmFactory)
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bares pendientes") },
                actions = { TextButton(onClick = onLogout) { Text("Salir") } }
            )
        }
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.error != null -> ErrorBox(state.error!!, onRetry = vm::load,
                modifier = Modifier.padding(padding))
            state.pending.isEmpty() -> EmptyBox("No hay bares pendientes 🎉",
                Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.pending) { bar ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(bar.name, style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text(bar.address, style = MaterialTheme.typography.bodyMedium)
                            bar.description?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { vm.approve(bar.id) }) { Text("Aprobar") }
                                OutlinedButton(onClick = { vm.reject(bar.id) }) { Text("Rechazar") }
                            }
                        }
                    }
                }
            }
        }
    }
}
