package com.matchbar.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.matchbar.app.ui.navigation.AppNavigation
import com.matchbar.app.ui.theme.MatchBarTheme

class MainActivity : ComponentActivity() {

    private val locationPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* sin acción específica; las pantallas que usan ubicación caen al fallback */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pedimos permisos de ubicación al iniciar (no bloqueante).
        locationPermissions.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))

        setContent {
            MatchBarTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(application as MatchBarApp)
                }
            }
        }
    }
}
