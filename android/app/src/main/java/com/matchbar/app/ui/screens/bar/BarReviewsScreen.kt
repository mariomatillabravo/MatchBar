package com.matchbar.app.ui.screens.bar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchbar.app.data.api.MatchBarApi
import com.matchbar.app.data.model.Review
import com.matchbar.app.ui.common.Appear
import com.matchbar.app.ui.common.EmptyBox
import com.matchbar.app.ui.common.ErrorBox
import com.matchbar.app.ui.common.LoadingBox
import com.matchbar.app.ui.common.brandGradient
import com.matchbar.app.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class BarReviewsUiState(
    val rating: Double? = null,
    val reviews: List<Review> = emptyList(),
    val loading: Boolean = false,
    val noBar: Boolean = false,
    val error: String? = null
)

class BarReviewsViewModel(private val api: MatchBarApi) : ViewModel() {
    private val _state = MutableStateFlow(BarReviewsUiState())
    val state: StateFlow<BarReviewsUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null, noBar = false) }
        runCatching {
            val bar = api.myBar()
            val reviews = api.barReviews(bar.id)
            bar.averageRating to reviews
        }.onSuccess { (rating, reviews) ->
            _state.update { it.copy(loading = false, rating = rating, reviews = reviews) }
        }.onFailure { e ->
            if (e is retrofit2.HttpException && e.code() == 404) {
                _state.update { it.copy(loading = false, noBar = true) }
            } else {
                _state.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarReviewsScreen(
    vmFactory: androidx.lifecycle.ViewModelProvider.Factory,
    onOpenSettings: () -> Unit
) {
    val vm: BarReviewsViewModel = viewModel(factory = vmFactory)
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis reseñas", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, "Ajustes")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.noBar -> EmptyBox("Crea primero la ficha de tu bar", Modifier.padding(padding))
            state.error != null -> ErrorBox(state.error!!, onRetry = vm::load,
                modifier = Modifier.padding(padding))
            else -> Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // La valoración siempre arriba
                RatingBanner(rating = state.rating, count = state.reviews.size,
                    modifier = Modifier.padding(16.dp))

                if (state.reviews.isEmpty()) {
                    EmptyBox("Tu bar aún no tiene reseñas")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(state.reviews) { index, r ->
                            Appear(indexForStagger = index) { ReviewCard(r) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingBanner(rating: Double?, count: Int, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
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
                val filled = (rating ?: 0.0).roundToInt().coerceIn(0, 5)
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
                Spacer(Modifier.height(6.dp))
                Text(
                    if (count == 0) "Sin valoraciones todavía"
                    else "$count ${if (count == 1) "valoración" else "valoraciones"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(r: Review) {
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
