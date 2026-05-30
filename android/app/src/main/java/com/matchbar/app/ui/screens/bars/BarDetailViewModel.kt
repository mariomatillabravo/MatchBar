package com.matchbar.app.ui.screens.bars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matchbar.app.data.api.MatchBarApi
import com.matchbar.app.data.model.Bar
import com.matchbar.app.data.model.Match
import com.matchbar.app.data.model.Review
import com.matchbar.app.data.model.ReviewRequest
import com.matchbar.app.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BarDetailUiState(
    val bar: Bar? = null,
    val reviews: List<Review> = emptyList(),
    val matches: List<Match> = emptyList(),
    val isFavorite: Boolean = false,
    val loading: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

class BarDetailViewModel(
    private val api: MatchBarApi
) : ViewModel() {

    private val _state = MutableStateFlow(BarDetailUiState())
    val state: StateFlow<BarDetailUiState> = _state.asStateFlow()

    fun load(barId: String) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            val bar = api.bar(barId)
            val reviews = api.barReviews(barId)
            val matches = runCatching { api.barUpcomingMatches(barId) }.getOrDefault(emptyList())
            val favs = runCatching { api.favorites() }.getOrDefault(emptyList())
            LoadResult(bar, reviews, matches, favs.any { it.id == barId })
        }.onSuccess { (b, r, m, fav) ->
            _state.update { it.copy(loading = false, bar = b, reviews = r, matches = m, isFavorite = fav) }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, error = e.userMessage()) }
        }
    }

    fun toggleFavorite() = viewModelScope.launch {
        val barId = _state.value.bar?.id ?: return@launch
        val wasFav = _state.value.isFavorite
        _state.update { it.copy(isFavorite = !wasFav) } // optimista
        runCatching {
            if (wasFav) api.removeFavorite(barId) else api.addFavorite(barId)
        }.onFailure { e ->
            _state.update { it.copy(isFavorite = wasFav, error = e.userMessage()) }
        }
    }

    fun submitReview(atmosphere: Int, food: Int, price: Int, comment: String) = viewModelScope.launch {
        val barId = _state.value.bar?.id ?: return@launch
        _state.update { it.copy(submitting = true, error = null, info = null) }
        runCatching { api.addReview(barId, ReviewRequest(atmosphere, food, price, comment.ifBlank { null })) }
            .onSuccess { newReview ->
                _state.update {
                    it.copy(
                        submitting = false,
                        reviews = listOf(newReview) + it.reviews,
                        info = "¡Valoración enviada!"
                    )
                }
            }
            .onFailure { e ->
                _state.update { it.copy(submitting = false, error = e.userMessage()) }
            }
    }

    fun clearMessages() = _state.update { it.copy(info = null, error = null) }

    private data class LoadResult(
        val bar: Bar,
        val reviews: List<Review>,
        val matches: List<Match>,
        val isFavorite: Boolean
    )
}
