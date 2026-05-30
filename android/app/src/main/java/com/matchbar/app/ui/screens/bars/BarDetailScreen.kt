package com.matchbar.app.ui.screens.bars

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.matchbar.app.data.model.Match
import com.matchbar.app.data.model.Review
import com.matchbar.app.data.model.Role
import com.matchbar.app.ui.common.Appear
import com.matchbar.app.ui.common.ErrorBox
import com.matchbar.app.ui.common.LoadingBox
import com.matchbar.app.ui.common.brandGradient
import com.matchbar.app.util.absoluteUrl
import com.matchbar.app.util.formatKickoff
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarDetailScreen(
    barId: String,
    currentRole: Role,
    vmFactory: androidx.lifecycle.ViewModelProvider.Factory,
    onBack: () -> Unit
) {
    val vm: BarDetailViewModel = viewModel(factory = vmFactory)
    val state by vm.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    var fullscreenImage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(barId) { vm.load(barId) }
    LaunchedEffect(state.info, state.error) {
        state.info?.let { snackbarHost.showSnackbar(it); vm.clearMessages() }
        state.error?.let { snackbarHost.showSnackbar(it); vm.clearMessages() }
    }

    val favScale by animateFloatAsState(
        targetValue = if (state.isFavorite) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fav"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text(state.bar?.name ?: "Bar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    if (currentRole == Role.USER && state.bar != null) {
                        IconButton(onClick = vm::toggleFavorite) {
                            Icon(
                                if (state.isFavorite) Icons.Filled.Favorite
                                else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = if (state.isFavorite) MaterialTheme.colorScheme.error
                                else LocalContentColor.current,
                                modifier = Modifier.graphicsLayer { scaleX = favScale; scaleY = favScale }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.bar == null && state.error != null ->
                ErrorBox(state.error!!, onRetry = { vm.load(barId) }, modifier = Modifier.padding(padding))
            state.bar != null -> {
                val bar = state.bar!!
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Appear {
                        RatingHeader(rating = bar.averageRating, reviewCount = state.reviews.size)
                    }

                    Spacer(Modifier.height(14.dp))

                    Appear(indexForStagger = 1) {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                bar.description?.let {
                                    Text(it, style = MaterialTheme.typography.bodyLarge)
                                    Spacer(Modifier.height(12.dp))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Place, null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(bar.address, style = MaterialTheme.typography.bodyMedium)
                                }
                                // Teléfono: solo si el bar lo ha indicado
                                bar.ownerPhone?.takeIf { it.isNotBlank() }?.let { phone ->
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            context.startActivity(
                                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                                        }
                                    ) {
                                        Icon(Icons.Filled.Phone, null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(phone, style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    // ----- Partidos que retransmite -----
                    Spacer(Modifier.height(14.dp))
                    Appear(indexForStagger = 2) {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.SportsSoccer, null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Partidos que retransmite",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(10.dp))
                                if (state.matches.isEmpty()) {
                                    Text("Este bar no tiene partidos programados por ahora.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    state.matches.forEachIndexed { i, m ->
                                        if (i > 0) Spacer(Modifier.height(8.dp))
                                        MatchRow(m)
                                    }
                                }
                            }
                        }
                    }

                    // ----- Fotos del establecimiento -----
                    if (bar.photoUrls.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Appear(indexForStagger = 2) {
                            Column {
                                Text("FOTOS", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(bar.photoUrls) { url ->
                                        AsyncImage(
                                            model = absoluteUrl(url),
                                            contentDescription = "Foto del bar",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(width = 230.dp, height = 150.dp)
                                                .clip(MaterialTheme.shapes.large)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { fullscreenImage = absoluteUrl(url) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ----- Carta del bar -----
                    bar.menuUrl?.let { menu ->
                        Spacer(Modifier.height(14.dp))
                        Appear(indexForStagger = 3) {
                            ElevatedCard(
                                onClick = { fullscreenImage = absoluteUrl(menu) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.MenuBook, null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(26.dp))
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Carta del bar",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold)
                                        Text("Toca para verla",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    if (currentRole == Role.USER) {
                        Spacer(Modifier.height(20.dp))
                        Appear(indexForStagger = 4) {
                            ReviewForm(
                                submitting = state.submitting,
                                onSubmit = { a, f, p, c -> vm.submitReview(a, f, p, c) }
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("Valoraciones (${state.reviews.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    if (state.reviews.isEmpty()) {
                        Text("Aún no hay valoraciones. ¡Sé el primero!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        state.reviews.forEachIndexed { i, r ->
                            Appear(indexForStagger = i) { ReviewRow(r) }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Imagen a pantalla completa (fotos o carta)
                fullscreenImage?.let { img ->
                    Dialog(onDismissRequest = { fullscreenImage = null }) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            AsyncImage(
                                model = img,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.large)
                            )
                            IconButton(onClick = { fullscreenImage = null }) {
                                Icon(Icons.Filled.Close, "Cerrar",
                                    tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingHeader(rating: Double?, reviewCount: Int) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(brandGradient()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    rating?.let { "%.1f".format(it) } ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(18.dp))
            Column {
                StarsRow(rating ?: 0.0)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (reviewCount == 0) "Sin valoraciones todavía"
                    else "$reviewCount ${if (reviewCount == 1) "valoración" else "valoraciones"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StarsRow(rating: Double) {
    val filled = rating.roundToInt().coerceIn(0, 5)
    Row {
        for (i in 1..5) {
            Icon(
                if (i <= filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ReviewForm(
    submitting: Boolean,
    onSubmit: (atmosphere: Int, food: Int, price: Int, comment: String) -> Unit
) {
    var atmosphere by remember { mutableIntStateOf(4) }
    var food by remember { mutableIntStateOf(4) }
    var price by remember { mutableIntStateOf(3) }
    var comment by remember { mutableStateOf("") }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Deja tu valoración",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            StarSelector("Ambiente", atmosphere) { atmosphere = it }
            StarSelector("Comida", food) { food = it }
            StarSelector("Precio", price) { price = it }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = comment,
                onValueChange = { if (it.length <= 500) comment = it },
                label = { Text("Comentario (opcional)") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onSubmit(atmosphere, food, price, comment) },
                enabled = !submitting,
                modifier = Modifier.align(Alignment.End)
            ) {
                if (submitting) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else Text("Enviar")
            }
        }
    }
}

@Composable
private fun StarSelector(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        for (i in 1..5) {
            val selected = i <= value
            val scale by animateFloatAsState(
                targetValue = if (selected) 1f else 0.82f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "star$i"
            )
            IconButton(onClick = { onChange(i) }, modifier = Modifier.size(34.dp)) {
                Icon(
                    if (selected) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$i",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                )
            }
        }
    }
}

@Composable
private fun MatchRow(match: Match) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "${match.homeTeamName} · ${match.awayTeamName}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                match.competitionName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text(
                formatKickoff(match.kickoffAt),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ReviewRow(r: Review) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        r.userName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(r.userName, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Star, null, modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(2.dp))
                    Text("%.1f".format(r.average), style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            r.comment?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text("Ambiente ${r.ratingAtmosphere} · Comida ${r.ratingFood} · Precio ${r.ratingPrice}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
