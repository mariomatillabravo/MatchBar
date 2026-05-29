package com.matchbar.app.ui.screens.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.matchbar.app.data.model.Bar
import com.matchbar.app.ui.common.ErrorBox
import com.matchbar.app.ui.common.LoadingBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyBarsScreen(
    matchId: String?,
    vmFactory: androidx.lifecycle.ViewModelProvider.Factory,
    onBarClick: (Bar) -> Unit,
    onBack: () -> Unit
) {
    val vm: NearbyBarsViewModel = viewModel(factory = vmFactory)
    val state by vm.state.collectAsState()

    LaunchedEffect(matchId) { vm.init(matchId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (matchId == null) "Bares cercanos" else "Dónde ver el partido") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> LoadingBox(Modifier.weight(1f))
                state.error != null -> ErrorBox(state.error!!,
                    onRetry = vm::loadLocationAndBars,
                    modifier = Modifier.weight(1f))
                else -> {
                    val center = LatLng(
                        state.userLat ?: NearbyBarsViewModel.DEFAULT_LAT,
                        state.userLng ?: NearbyBarsViewModel.DEFAULT_LNG
                    )
                    val cameraState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(center, 13f)
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraState
                        ) {
                            state.bars.forEach { bar ->
                                Marker(
                                    state = MarkerState(LatLng(bar.latitude, bar.longitude)),
                                    title = bar.name,
                                    snippet = bar.address,
                                    onInfoWindowClick = { onBarClick(bar) }
                                )
                            }
                        }
                    }

                    HorizontalDivider()
                    Text("${state.bars.size} bares encontrados",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(16.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.bars) { bar -> BarRow(bar) { onBarClick(bar) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarRow(bar: Bar, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(bar.name, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Text(bar.address, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.padding(top = 6.dp)) {
                bar.distanceMeters?.let {
                    Text("📍 ${it.toInt()} m", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(12.dp))
                }
                bar.averageRating?.let {
                    Text("⭐ %.1f".format(it), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
