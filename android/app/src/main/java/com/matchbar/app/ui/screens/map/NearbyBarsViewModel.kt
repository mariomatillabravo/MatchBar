package com.matchbar.app.ui.screens.map

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.matchbar.app.data.api.MatchBarApi
import com.matchbar.app.data.model.Bar
import com.matchbar.app.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class NearbyUiState(
    val bars: List<Bar> = emptyList(),
    val userLat: Double? = null,
    val userLng: Double? = null,
    val matchId: Long? = null,
    val loading: Boolean = false,
    val error: String? = null
)

class NearbyBarsViewModel(
    private val api: MatchBarApi,
    private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(NearbyUiState())
    val state: StateFlow<NearbyUiState> = _state.asStateFlow()

    fun init(matchId: Long?) {
        _state.update { it.copy(matchId = matchId) }
        loadLocationAndBars()
    }

    fun loadLocationAndBars() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            val location = getLastLocation()
            val lat = location?.first ?: DEFAULT_LAT
            val lng = location?.second ?: DEFAULT_LNG
            _state.update { it.copy(userLat = lat, userLng = lng) }
            api.nearbyBars(lat, lng, _state.value.matchId, radiusMeters = 10_000)
        }.onSuccess { bars ->
            _state.update { it.copy(loading = false, bars = bars) }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, error = e.userMessage()) }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(): Pair<Double, Double>? = suspendCancellableCoroutine { cont ->
        try {
            val client = LocationServices.getFusedLocationProviderClient(appContext)
            client.lastLocation
                .addOnSuccessListener { loc ->
                    cont.resume(loc?.let { it.latitude to it.longitude })
                }
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
