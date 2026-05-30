package com.matchbar.app.ui.screens.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchbar.app.data.model.Bar
import com.matchbar.app.data.model.Match
import com.matchbar.app.ui.common.ErrorBox
import com.matchbar.app.ui.common.LoadingBox
import com.matchbar.app.util.barMarkerIcon
import com.matchbar.app.util.formatKickoff
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllBarsMapScreen(
    vmFactory: androidx.lifecycle.ViewModelProvider.Factory,
    onBarClick: (Bar) -> Unit
) {
    val vm: AllBarsMapViewModel = viewModel(factory = vmFactory)
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    val markerIcon = remember { barMarkerIcon(context) }

    Scaffold(topBar = { TopAppBar(title = { Text("Mapa de bares") }) }) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> LoadingBox(Modifier.fillMaxSize())
                state.error != null -> ErrorBox(state.error!!, onRetry = vm::load,
                    modifier = Modifier.fillMaxSize())
                else -> {
                    val centerLat = state.userLat ?: AllBarsMapViewModel.DEFAULT_LAT
                    val centerLng = state.userLng ?: AllBarsMapViewModel.DEFAULT_LNG
                    var centered by remember { mutableStateOf(false) }

                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { mapView },
                        update = { map ->
                            if (!centered) {
                                map.controller.setZoom(12.0)
                                map.controller.setCenter(GeoPoint(centerLat, centerLng))
                                centered = true
                            }
                            map.overlays.clear()
                            state.bars.forEach { bar ->
                                val marker = Marker(map).apply {
                                    position = GeoPoint(bar.latitude, bar.longitude)
                                    title = bar.name
                                    snippet = bar.address
                                    markerIcon?.let {
                                        icon = it
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    }
                                    setOnMarkerClickListener { _, _ ->
                                        vm.selectBar(bar)
                                        true // consumimos el evento: no mostramos info-window nativa
                                    }
                                }
                                map.overlays.add(marker)
                            }
                            map.invalidate()
                        }
                    )
                }
            }
        }
    }

    state.selectedBar?.let { bar ->
        BarMatchesSheet(
            bar = bar,
            matches = state.barMatches,
            loading = state.loadingMatches,
            error = state.matchesError,
            onDismiss = vm::dismissSheet,
            onSeeBar = { vm.dismissSheet(); onBarClick(bar) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarMatchesSheet(
    bar: Bar,
    matches: List<Match>,
    loading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSeeBar: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(bar.name, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Text(bar.address, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            bar.averageRating?.let {
                Spacer(Modifier.height(4.dp))
                Text("⭐ %.1f".format(it), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onSeeBar, modifier = Modifier.fillMaxWidth()) {
                Text("Ver bar")
            }

            Spacer(Modifier.height(16.dp))
            Text("Próximos partidos (15 días)",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            when {
                loading -> Box(Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                }
                error != null -> Text(error, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
                matches.isEmpty() -> Text(
                    "No retransmite partidos en los próximos 15 días",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(matches) { m -> MatchRow(m) }
                }
            }
        }
    }
}

@Composable
private fun MatchRow(match: Match) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SportsSoccer, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(match.competitionName, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                Text(formatKickoff(match.kickoffAt), style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text("${match.homeTeamName}  vs  ${match.awayTeamName}",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}
