package com.balltown.predictrivals.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.TournamentRepository
import com.balltown.predictrivals.domain.model.Tournament
import com.balltown.predictrivals.res.Res
import com.balltown.predictrivals.res.action_join
import com.balltown.predictrivals.res.field_join_code
import com.balltown.predictrivals.res.join_tournament_title
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

sealed class JoinTournamentUiState {
    data object Idle : JoinTournamentUiState()
    data object Loading : JoinTournamentUiState()
    data class Joined(val tournament: Tournament) : JoinTournamentUiState()
    data class Error(val message: String) : JoinTournamentUiState()
}

class JoinTournamentViewModel(private val tournamentRepository: TournamentRepository) : ViewModel() {
    private val _state = MutableStateFlow<JoinTournamentUiState>(JoinTournamentUiState.Idle)
    val state: StateFlow<JoinTournamentUiState> = _state.asStateFlow()

    fun join(joinCode: String) {
        _state.value = JoinTournamentUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                JoinTournamentUiState.Joined(tournamentRepository.join(joinCode))
            } catch (e: ApiException) {
                JoinTournamentUiState.Error(e.message)
            }
        }
    }
}

@Composable
fun JoinTournamentScreen(onJoined: (Tournament) -> Unit, viewModel: JoinTournamentViewModel = koinViewModel()) {
    var joinCode by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    (state as? JoinTournamentUiState.Joined)?.let { onJoined(it.tournament) }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.join_tournament_title))
        OutlinedTextField(
            value = joinCode,
            onValueChange = { joinCode = it.uppercase() },
            label = { Text(stringResource(Res.string.field_join_code)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { viewModel.join(joinCode) }, enabled = state !is JoinTournamentUiState.Loading) { Text(stringResource(Res.string.action_join)) }
        if (state is JoinTournamentUiState.Loading) CircularProgressIndicator()
        if (state is JoinTournamentUiState.Error) Text((state as JoinTournamentUiState.Error).message)
    }
}
