package com.matchbar.app.ui.screens.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matchbar.app.data.api.MatchBarApi
import com.matchbar.app.data.model.Competition
import com.matchbar.app.data.model.Match
import com.matchbar.app.data.model.Team
import com.matchbar.app.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MatchesUiState(
    val matches: List<Match> = emptyList(),
    val competitions: List<Competition> = emptyList(),
    val teams: List<Team> = emptyList(),
    val selectedCompetitionId: String? = null,
    val selectedTeamId: String? = null,
    val loading: Boolean = false,
    val error: String? = null
)

class MatchesViewModel(private val api: MatchBarApi) : ViewModel() {

    private val _state = MutableStateFlow(MatchesUiState())
    val state: StateFlow<MatchesUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            val competitions = api.competitions()
            val teams = api.teams(_state.value.selectedCompetitionId)
            val matches = api.matches(
                competitionId = _state.value.selectedCompetitionId,
                teamId = _state.value.selectedTeamId
            )
            Triple(competitions, teams, matches)
        }.onSuccess { (c, t, m) ->
            _state.update { it.copy(loading = false, competitions = c, teams = t, matches = m) }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, error = e.userMessage()) }
        }
    }

    fun selectCompetition(id: String?) {
        _state.update { it.copy(selectedCompetitionId = id, selectedTeamId = null) }
        load()
    }

    fun selectTeam(id: String?) {
        _state.update { it.copy(selectedTeamId = id) }
        load()
    }
}
