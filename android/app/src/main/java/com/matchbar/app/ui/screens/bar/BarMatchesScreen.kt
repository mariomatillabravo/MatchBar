package com.matchbar.app.ui.screens.bar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchbar.app.data.api.MatchBarApi
import com.matchbar.app.data.model.Competition
import com.matchbar.app.data.model.Match
import com.matchbar.app.ui.common.AccentPill
import com.matchbar.app.ui.common.Appear
import com.matchbar.app.ui.common.EmptyBox
import com.matchbar.app.ui.common.ErrorBox
import com.matchbar.app.ui.common.LoadingBox
import com.matchbar.app.ui.common.brandGradient
import com.matchbar.app.util.formatKickoff
import com.matchbar.app.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

data class BarMatchesUiState(
    val competitions: List<Competition> = emptyList(),
    val selectedCompetitionId: String? = null,
    val matches: List<Match> = emptyList(),
    val scheduledIds: Set<String> = emptySet(),
    val loading: Boolean = false,
    val busyMatchId: String? = null,
    val error: String? = null,
    val info: String? = null
)

class BarMatchesViewModel(private val api: MatchBarApi) : ViewModel() {
    private val _state = MutableStateFlow(BarMatchesUiState())
    val state: StateFlow<BarMatchesUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            val comps = api.competitions()
            val scheduled = api.myScheduledMatches().map { it.id }.toSet()
            val matches = fetchMatches(_state.value.selectedCompetitionId)
            Triple(comps, scheduled, matches)
        }.onSuccess { (comps, scheduled, matches) ->
            _state.update {
                it.copy(loading = false, competitions = comps,
                    scheduledIds = scheduled, matches = matches)
            }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, error = e.userMessage()) }
        }
    }

    fun selectCompetition(id: String?) {
        _state.update { it.copy(selectedCompetitionId = id, loading = true) }
        viewModelScope.launch {
            runCatching { fetchMatches(id) }
                .onSuccess { m -> _state.update { it.copy(loading = false, matches = m) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.userMessage()) } }
        }
    }

    fun add(matchId: String) = viewModelScope.launch {
        _state.update { it.copy(busyMatchId = matchId) }
        runCatching { api.scheduleMatch(matchId) }
            .onSuccess {
                _state.update {
                    it.copy(busyMatchId = null,
                        scheduledIds = it.scheduledIds + matchId,
                        info = "Partido añadido a tu bar")
                }
            }
            .onFailure { e -> _state.update { it.copy(busyMatchId = null, error = e.userMessage()) } }
    }

    fun remove(matchId: String) = viewModelScope.launch {
        _state.update { it.copy(busyMatchId = matchId) }
        runCatching { api.cancelSchedule(matchId) }
            .onSuccess {
                _state.update {
                    it.copy(busyMatchId = null,
                        scheduledIds = it.scheduledIds - matchId,
                        info = "Partido eliminado de tu bar")
                }
            }
            .onFailure { e -> _state.update { it.copy(busyMatchId = null, error = e.userMessage()) } }
    }

    fun clearMessages() = _state.update { it.copy(info = null, error = null) }

    private suspend fun fetchMatches(competitionId: String?): List<Match> {
        val now = Instant.now()
        val until = now.plus(15, ChronoUnit.DAYS)
        // Filtramos los próximos 15 días en el cliente para no depender del
        // formato del parámetro de fecha del backend.
        return api.matches(competitionId = competitionId)
            .filter { m ->
                val k = runCatching { Instant.parse(m.kickoffAt) }.getOrNull()
                k == null || (!k.isBefore(now) && !k.isAfter(until))
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarMatchesScreen(
    vmFactory: androidx.lifecycle.ViewModelProvider.Factory,
    onOpenSettings: () -> Unit
) {
    val vm: BarMatchesViewModel = viewModel(factory = vmFactory)
    val state by vm.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.info, state.error) {
        state.info?.let { snackbarHost.showSnackbar(it); vm.clearMessages() }
        state.error?.let { snackbarHost.showSnackbar(it); vm.clearMessages() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Añadir partidos", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, "Ajustes")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.competitions.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedCompetitionId == null,
                            onClick = { vm.selectCompetition(null) },
                            label = { Text("Todas") }
                        )
                    }
                    items(state.competitions) { c ->
                        FilterChip(
                            selected = state.selectedCompetitionId == c.id,
                            onClick = { vm.selectCompetition(c.id) },
                            label = { Text(c.name) }
                        )
                    }
                }
            }

            when {
                state.loading -> LoadingBox(message = "Cargando partidos…")
                state.error != null && state.matches.isEmpty() ->
                    ErrorBox(state.error!!, onRetry = vm::load)
                state.matches.isEmpty() ->
                    EmptyBox("No hay partidos en los próximos 15 días")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(state.matches) { index, m ->
                        Appear(indexForStagger = index) {
                            BarMatchCard(
                                match = m,
                                added = m.id in state.scheduledIds,
                                busy = state.busyMatchId == m.id,
                                onAdd = { vm.add(m.id) },
                                onRemove = { vm.remove(m.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarMatchCard(
    match: Match,
    added: Boolean,
    busy: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(brandGradient())
                )
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AccentPill(
                            text = match.competitionName,
                            container = MaterialTheme.colorScheme.tertiaryContainer,
                            content = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Filled.Schedule, null, modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(formatKickoff(match.kickoffAt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(match.homeTeamName, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(36.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("VS", style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Text(match.awayTeamName, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    }
                    AnimatedVisibility(visible = added, enter = fadeIn(), exit = fadeOut()) {
                        Row(modifier = Modifier.padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("Lo retransmites en tu bar",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Botones Añadir / Eliminar al 50%
            Row(modifier = Modifier.fillMaxWidth().padding(start = 6.dp)) {
                Button(
                    onClick = onAdd,
                    enabled = !added && !busy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) {
                    if (busy && !added) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondary)
                    } else {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Añadir")
                    }
                }
                Button(
                    onClick = onRemove,
                    enabled = added && !busy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                ) {
                    if (busy && added) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError)
                    } else {
                        Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Eliminar")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
