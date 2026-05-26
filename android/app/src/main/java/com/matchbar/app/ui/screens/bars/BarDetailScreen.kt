package com.matchbar.app.ui.screens.bars

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchbar.app.data.model.Review
import com.matchbar.app.data.model.Role
import com.matchbar.app.ui.common.ErrorBox
import com.matchbar.app.ui.common.LoadingBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarDetailScreen(
    barId: Long,
    currentRole: Role,
    vmFactory: androidx.lifecycle.ViewModelProvider.Factory,
    onBack: () -> Unit
) {
    val vm: BarDetailViewModel = viewModel(factory = vmFactory)
    val state by vm.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(barId) { vm.load(barId) }
    LaunchedEffect(state.info, state.error) {
        state.info?.let { snackbarHost.showSnackbar(it); vm.clearMessages() }
        state.error?.let { snackbarHost.showSnackbar(it); vm.clearMessages() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text(state.bar?.name ?: "Bar") },
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
                                       else LocalContentColor.current
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
                    bar.description?.let {
                        Text(it, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(12.dp))
                    }
                    Text("📍 ${bar.address}", style = MaterialTheme.typography.bodyMedium)
                    bar.averageRating?.let {
                        Spacer(Modifier.height(4.dp))
                        Text("⭐ %.1f / 5".format(it), style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }

                    if (currentRole == Role.USER) {
                        Spacer(Modifier.height(24.dp))
                        ReviewForm(
                            submitting = state.submitting,
                            onSubmit = { a, f, p, c -> vm.submitReview(a, f, p, c) }
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("Valoraciones (${state.reviews.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    if (state.reviews.isEmpty()) {
                        Text("Aún no hay valoraciones. ¡Sé el primero!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        state.reviews.forEach { ReviewRow(it); Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
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
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            StarRow("Ambiente", atmosphere) { atmosphere = it }
            StarRow("Comida", food) { food = it }
            StarRow("Precio", price) { price = it }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = comment,
                onValueChange = { if (it.length <= 500) comment = it },
                label = { Text("Comentario (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            Spacer(Modifier.height(8.dp))
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
private fun StarRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        for (i in 1..5) {
            IconButton(onClick = { onChange(i) }, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (i <= value) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$i",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ReviewRow(r: Review) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row {
                Text(r.userName, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Text("⭐ %.1f".format(r.average), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            r.comment?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            Text("Ambiente ${r.ratingAtmosphere} · Comida ${r.ratingFood} · Precio ${r.ratingPrice}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
