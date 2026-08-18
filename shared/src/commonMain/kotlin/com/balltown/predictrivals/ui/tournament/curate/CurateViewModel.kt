package com.balltown.predictrivals.ui.tournament.curate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.CurationRepository
import com.balltown.predictrivals.data.repository.FixtureRepository
import com.balltown.predictrivals.domain.model.Fixture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CurateUiState {
    data object Idle : CurateUiState()
    data object Loading : CurateUiState()
    data class CandidatesLoaded(val candidates: List<Fixture>, val selected: Set<Int>) : CurateUiState()
    data object RoundCreated : CurateUiState()
    data class Error(val message: String) : CurateUiState()
}

class CurateViewModel(
    private val fixtureRepository: FixtureRepository,
    private val curationRepository: CurationRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<CurateUiState>(CurateUiState.Idle)
    val state: StateFlow<CurateUiState> = _state.asStateFlow()

    fun search(from: String, to: String) {
        _state.value = CurateUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                CurateUiState.CandidatesLoaded(fixtureRepository.candidates(from, to, search = null), selected = emptySet())
            } catch (e: ApiException) {
                CurateUiState.Error(e.message)
            }
        }
    }

    fun toggleSelected(fixtureId: Int) {
        val current = _state.value as? CurateUiState.CandidatesLoaded ?: return
        val newSelection = if (fixtureId in current.selected) current.selected - fixtureId else current.selected + fixtureId
        _state.value = current.copy(selected = newSelection)
    }

    fun createRound(tournamentId: Int) {
        val current = _state.value as? CurateUiState.CandidatesLoaded ?: return
        if (current.selected.size != 9) {
            _state.value = CurateUiState.Error("Pick exactly 9 fixtures (currently ${current.selected.size}).")
            return
        }
        viewModelScope.launch {
            _state.value = try {
                curationRepository.createRound(tournamentId, current.selected.toList())
                CurateUiState.RoundCreated
            } catch (e: ApiException) {
                CurateUiState.Error(e.message)
            }
        }
    }
}
