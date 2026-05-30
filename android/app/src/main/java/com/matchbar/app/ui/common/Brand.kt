package com.matchbar.app.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchbar.app.R
import kotlinx.coroutines.delay

/**
 * Componentes y animaciones de marca reutilizables.
 * Centralizamos aquí lo "vistoso" para mantener coherencia en toda la app.
 */

/** Degradado de marca verde → azul, deportivo y con energía. */
@Composable
fun brandGradient(): Brush = Brush.linearGradient(
    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
)

/**
 * Envuelve contenido con una entrada animada (fundido + deslizamiento hacia arriba).
 * Pásale un índice para conseguir un efecto en cascada en listas.
 */
@Composable
fun Appear(
    indexForStagger: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((indexForStagger.coerceAtMost(8) * 60).toLong())
        shown = true
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "appear"
    )
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 28.dp.toPx()
        }
    ) { content() }
}

/** Insignia de marca: balón sobre un círculo en degradado. Con pulso opcional. */
@Composable
fun BrandBadge(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    pulse: Boolean = false
) {
    val infinite = rememberInfiniteTransition(label = "badge")
    val pulseScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val gradient = brandGradient()
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                if (pulse) {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
            }
            .clip(CircleShape)
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.SportsSoccer,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/**
 * Logo de marca personalizado (imagen) con un pulso lento opcional.
 * Reutiliza la misma animación de escala que [BrandBadge] para mantener
 * coherencia visual en la pantalla de login.
 */
@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    pulse: Boolean = false
) {
    val infinite = rememberInfiniteTransition(label = "logo")
    val pulseScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "logoPulse"
    )
    Image(
        painter = painterResource(R.drawable.brand_logo),
        contentDescription = "MatchBar",
        modifier = modifier
            .size(size)
            .graphicsLayer {
                if (pulse) {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
            }
    )
}

/** Botón principal con degradado de marca y estado de carga. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f, label = "btnScale")
    val shape = MaterialTheme.shapes.large

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(
                if (enabled) brandGradient()
                else Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !loading,
                onClick = onClick
            )
            .heightIn(min = 54.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                leadingIcon?.let {
                    Icon(it, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Pequeña "píldora" de etiqueta con color de acento. */
@Composable
fun AccentPill(
    text: String,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text.uppercase(),
            color = content,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

/** Escala el contenido al pulsarlo, para una micro-interacción agradable (sin ripple). */
fun Modifier.bounceClick(onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "bounce")
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}
