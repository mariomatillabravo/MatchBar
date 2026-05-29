package com.matchbar.app.ui.screens.favorites

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

data class FavoritesUiState(
    val bars: List<Bar> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class FavoritesViewModel(private val api: MatchBarApi) : ViewModel() {
    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching { api.favorites() }
            .onSuccess { _state.update { st -> st.copy(loading = false, bars = it) } }
            .onFailure { e -> _state.update { it.copy(loading = false, error = e.userMessage()) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    vmFactory: androidx.lifecycle.ViewModelProvider.Factory,
    onBarClick: (Bar) -> Unit
) {
    val vm: FavoritesViewModel = viewModel(factory = vmFactory)
    val state by vm.state.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Mis favoritos") }) }) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.error != null -> ErrorBox(state.error!!, onRetry = vm::load,
                modifier = Modifier.padding(padding))
            state.bars.isEmpty() -> EmptyBox("Aún no has marcado ningún bar como favorito",
                Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.bars) { b ->
                    ElevatedCard(onClick = { onBarClick(b) }, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(b.name, style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text(b.address, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            b.averageRating?.let {
                                Spacer(Modifier.height(4.dp))
                                Text("⭐ %.1f".format(it),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
