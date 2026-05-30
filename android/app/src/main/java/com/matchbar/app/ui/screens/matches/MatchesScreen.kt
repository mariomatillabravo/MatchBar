package com.matchbar.app.ui.screens.matches

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchbar.app.data.model.Match
import com.matchbar.app.ui.common.AccentPill
import com.matchbar.app.ui.common.Appear
import com.matchbar.app.ui.common.EmptyBox
import com.matchbar.app.ui.common.ErrorBox
import com.matchbar.app.ui.common.LoadingBox
import com.matchbar.app.ui.common.brandGradient
import com.matchbar.app.util.formatKickoff

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    vmFactory: androidx.lifecycle.ViewModelProvider.Factory
) {
    val vm: MatchesViewModel = viewModel(factory = vmFactory)
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text("Partidos", fontWeight = FontWeight.ExtraBold)
            })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Filtros por competición
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
                state.error != null -> ErrorBox(state.error!!, onRetry = vm::load)
                state.matches.isEmpty() -> EmptyBox("No hay partidos para los filtros seleccionados")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(state.matches) { index, m ->
                        Appear(indexForStagger = index) {
                            MatchCard(m)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchCard(match: Match) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Franja de acento en degradado a la izquierda
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
                    Icon(
                        Icons.Filled.Schedule, null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        formatKickoff(match.kickoffAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        match.homeTeamName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                    VsBadge(modifier = Modifier.padding(horizontal = 12.dp))
                    Text(
                        match.awayTeamName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

            }
        }
    }
}

@Composable
private fun VsBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "VS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
