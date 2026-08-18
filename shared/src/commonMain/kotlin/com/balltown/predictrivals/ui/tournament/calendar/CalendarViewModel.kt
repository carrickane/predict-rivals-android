package com.balltown.predictrivals.ui.tournament.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.CalendarRepository
import com.balltown.predictrivals.domain.model.Round
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CalendarUiState {
    data object Loading : CalendarUiState()
    data class Loaded(val rounds: List<Round>) : CalendarUiState()
    data class Error(val message: String) : CalendarUiState()
}

class CalendarViewModel(private val calendarRepository: CalendarRepository) : ViewModel() {
    private val _state = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    fun load(tournamentId: Int) {
        viewModelScope.launch {
            _state.value = try {
                CalendarUiState.Loaded(calendarRepository.calendar(tournamentId))
            } catch (e: ApiException) {
                CalendarUiState.Error(e.message)
            }
        }
    }
}
