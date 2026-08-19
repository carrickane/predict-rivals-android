package com.balltown.predictrivals.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.TournamentRepository
import com.balltown.predictrivals.domain.model.Tournament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Loaded(val tournaments: List<Tournament>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(private val tournamentRepository: TournamentRepository) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _state.value = try {
                HomeUiState.Loaded(tournamentRepository.mine())
            } catch (e: ApiException) {
                HomeUiState.Error(e.message)
            }
            _isRefreshing.value = false
        }
    }

    /** Starts a tournament early, then reloads the list so its status reflects the change in place. */
    fun startEarly(tournamentId: Int) {
        viewModelScope.launch {
            try {
                tournamentRepository.start(tournamentId)
                refresh()
            } catch (e: ApiException) {
                // No inline error surface for a single list-item action yet — the tournament
                // simply stays "open" in the list, matching the unchanged server state.
            }
        }
    }
}
