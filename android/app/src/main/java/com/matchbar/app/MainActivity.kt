package com.matchbar.app

import android.Manifest
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.matchbar.app.data.local.ThemeMode
import com.matchbar.app.ui.navigation.AppNavigation
import com.matchbar.app.ui.theme.MatchBarTheme

class MainActivity : ComponentActivity() {

    private val locationPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* sin acción específica; las pantallas que usan ubicación caen al fallback */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dibujamos detrás de las barras del sistema (obligatorio con targetSdk 35+);
        // cada pantalla deja sitio con sus insets (Scaffold/TopAppBar).
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Pedimos permisos de ubicación al iniciar (no bloqueante).
        locationPermissions.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))

        val app = application as MatchBarApp

        setContent {
            val themeMode by app.sessionStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Los iconos de las barras del sistema siguen el tema de la app, que el
            // usuario puede fijar en claro/oscuro aunque el sistema use el otro.
            DisposableEffect(darkTheme) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            MatchBarTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(app)
                }
            }
        }
    }
}
