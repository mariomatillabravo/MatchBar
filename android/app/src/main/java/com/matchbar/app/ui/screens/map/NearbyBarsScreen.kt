package com.matchbar.app.ui.screens.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchbar.app.data.model.Bar
import com.matchbar.app.ui.common.ErrorBox
import com.matchbar.app.ui.common.LoadingBox
import com.matchbar.app.util.barMarkerIcon
import com.matchbar.app.util.userMarkerIcon
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

/** Zoom al que acercamos el mapa cuando el usuario selecciona un bar de la lista. */
private const val FOCUS_ZOOM = 16.0
private const val OVERVIEW_ZOOM = 13.0
private const val FOCUS_ANIM_MS = 1200L

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
    val mapView = rememberMapViewWithLifecycle()
    val context = LocalContext.current
    val markerIcon = remember { barMarkerIcon(context) }
    val userIcon = remember { userMarkerIcon(context) }

    // Bar seleccionado desde la lista (para resaltarlo y centrar el mapa en él).
    var selectedBarId by remember { mutableStateOf<String?>(null) }

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
                    val centerLat = state.userLat ?: NearbyBarsViewModel.DEFAULT_LAT
                    val centerLng = state.userLng ?: NearbyBarsViewModel.DEFAULT_LNG
                    var centered by remember { mutableStateOf(false) }

                    // ----- 50% superior: MAPA -----
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { mapView },
                            update = { map ->
                                if (!centered) {
                                    map.controller.setZoom(OVERVIEW_ZOOM)
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
                                        // Tocar el pin del mapa abre la ficha del bar.
                                        setOnMarkerClickListener { _, _ ->
                                            onBarClick(bar)
                                            true
                                        }
                                    }
                                    map.overlays.add(marker)
                                }
                                // Marcador del usuario (solo si tenemos su ubicación real).
                                val uLat = state.userLat
                                val uLng = state.userLng
                                if (uLat != null && uLng != null) {
                                    val me = Marker(map).apply {
                                        position = GeoPoint(uLat, uLng)
                                        title = "Tu ubicación"
                                        icon = userIcon
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        setOnMarkerClickListener { _, _ -> true }
                                    }
                                    map.overlays.add(me)
                                }
                                map.invalidate()
                            }
                        )
                    }

                    // ----- 50% inferior: LISTA DE BARES -----
                    Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        HorizontalDivider()
                        Text(
                            if (state.bars.isEmpty()) "Ningún bar retransmite este partido"
                            else "${state.bars.size} bares retransmiten este partido",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )

                        if (state.bars.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "Aún no hay bares que emitan este partido cerca de ti.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.bars) { bar ->
                                    BarRow(bar = bar, selected = bar.id == selectedBarId) {
                                        // Tocar un bar de la lista centra el mapa en él con un
                                        // leve zoom, sin abrir la ficha.
                                        selectedBarId = bar.id
                                        mapView.controller.animateTo(
                                            GeoPoint(bar.latitude, bar.longitude),
                                            FOCUS_ZOOM,
                                            FOCUS_ANIM_MS
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarRow(bar: Bar, selected: Boolean, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (selected) CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.elevatedCardColors()
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
            Icon(
                Icons.Filled.Place,
                contentDescription = "Ver en el mapa",
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
