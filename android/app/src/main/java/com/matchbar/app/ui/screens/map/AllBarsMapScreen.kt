package com.matchbar.app.ui.screens.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.matchbar.app.R
import com.matchbar.app.data.model.Bar
import com.matchbar.app.data.model.Match
import com.matchbar.app.ui.common.ErrorBox
import com.matchbar.app.ui.common.LoadingBox
import com.matchbar.app.util.formatKickoff

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllBarsMapScreen(
    vmFactory: androidx.lifecycle.ViewModelProvider.Factory,
    onBarClick: (Bar) -> Unit
) {
    val vm: AllBarsMapViewModel = viewModel(factory = vmFactory)
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val markerIcon = remember { bitmapDescriptorFromVector(context, R.drawable.ic_bar_marker) }

    Scaffold(topBar = { TopAppBar(title = { Text("Mapa de bares") }) }) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> LoadingBox(Modifier.fillMaxSize())
                state.error != null -> ErrorBox(state.error!!, onRetry = vm::load,
                    modifier = Modifier.fillMaxSize())
                else -> {
                    val center = LatLng(
                        state.userLat ?: AllBarsMapViewModel.DEFAULT_LAT,
                        state.userLng ?: AllBarsMapViewModel.DEFAULT_LNG
                    )
                    val cameraState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(center, 12f)
                    }
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraState
                    ) {
                        state.bars.forEach { bar ->
                            Marker(
                                state = MarkerState(LatLng(bar.latitude, bar.longitude)),
                                title = bar.name,
                                snippet = bar.address,
                                icon = markerIcon,
                                onClick = {
                                    vm.selectBar(bar)
                                    true // consumimos el evento: no mostramos info-window nativa
                                }
                            )
                        }
                    }
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

/** Convierte un vector drawable en un BitmapDescriptor para usarlo como icono de marcador. */
private fun bitmapDescriptorFromVector(context: Context, resId: Int): BitmapDescriptor? {
    // Garantiza que la fábrica de BitmapDescriptor esté inicializada aunque el mapa
    // todavía no se haya creado (evita "IBitmapDescriptorFactory is not initialized").
    MapsInitializer.initialize(context)
    val drawable = ContextCompat.getDrawable(context, resId) ?: return null
    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
    )
    drawable.draw(Canvas(bitmap))
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
