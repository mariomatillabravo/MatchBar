package com.matchbar.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF1F3864)
private val PrimaryDark = Color(0xFF142547)
private val Secondary = Color(0xFF2E74B5)
private val OnPrimary = Color.White

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    onSecondary = OnPrimary,
    background = Color(0xFFF5F7FA),
    surface = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Secondary,
    onPrimary = OnPrimary,
    secondary = Primary,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

@Composable
fun MatchBarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
