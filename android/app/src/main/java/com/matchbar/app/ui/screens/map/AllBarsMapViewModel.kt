package com.matchbar.app.ui.screens.map

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.matchbar.app.data.api.MatchBarApi
import com.matchbar.app.data.model.Bar
import com.matchbar.app.data.model.Match
import com.matchbar.app.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class AllBarsMapUiState(
    val bars: List<Bar> = emptyList(),
    val userLat: Double? = null,
    val userLng: Double? = null,
    val loading: Boolean = false,
    val error: String? = null,
    // Bottom sheet
    val selectedBar: Bar? = null,
    val barMatches: List<Match> = emptyList(),
    val loadingMatches: Boolean = false,
    val matchesError: String? = null
)

class AllBarsMapViewModel(
    private val api: MatchBarApi,
    private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(AllBarsMapUiState())
    val state: StateFlow<AllBarsMapUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            val location = getLastLocation()
            _state.update { it.copy(userLat = location?.first, userLng = location?.second) }
            api.allBars()
        }.onSuccess { bars ->
            _state.update { it.copy(loading = false, bars = bars) }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, error = e.userMessage()) }
        }
    }

    fun selectBar(bar: Bar) {
        _state.update {
            it.copy(selectedBar = bar, barMatches = emptyList(),
                loadingMatches = true, matchesError = null)
        }
        viewModelScope.launch {
            runCatching { api.barUpcomingMatches(bar.id) }
                .onSuccess { matches ->
                    _state.update { it.copy(loadingMatches = false, barMatches = matches) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loadingMatches = false, matchesError = e.userMessage()) }
                }
        }
    }

    fun dismissSheet() {
        _state.update { it.copy(selectedBar = null, barMatches = emptyList(), matchesError = null) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(): Pair<Double, Double>? = suspendCancellableCoroutine { cont ->
        try {
            val client = LocationServices.getFusedLocationProviderClient(appContext)
            client.lastLocation
                .addOnSuccessListener { loc -> cont.resume(loc?.let { it.latitude to it.longitude }) }
                .addOnFailureListener { cont.resume(null) }
        } catch (e: SecurityException) {
            cont.resume(null)
        }
    }

    companion object {
        // Madrid centro como fallback
        const val DEFAULT_LAT = 40.4168
        const val DEFAULT_LNG = -3.7038
    }
}
