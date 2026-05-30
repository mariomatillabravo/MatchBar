package com.matchbar.app.ui.screens.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

/**
 * Crea un [MapView] de osmdroid (OpenStreetMap, sin API key) y lo ata al ciclo
 * de vida del composable: reenvía onResume/onPause y libera recursos al salir.
 */
@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        // osmdroid usa por defecto el User-Agent "osmdroid", que los servidores
        // de tiles de OpenStreetMap bloquean (no se cargarían los mapas). Hay que
        // identificarse con el id del paquete antes de crear el MapView.
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
            userAgentValue = context.packageName
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            // Ocultamos los botones de zoom; se usa pellizco/doble toque.
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }
    return mapView
}
