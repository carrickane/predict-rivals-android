package com.balltown.predictrivals.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.TournamentRepository
import com.balltown.predictrivals.domain.model.Tournament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

sealed class CreateTournamentUiState {
    data object Idle : CreateTournamentUiState()
    data object Loading : CreateTournamentUiState()
    data class Created(val tournament: Tournament) : CreateTournamentUiState()
    data class Error(val message: String) : CreateTournamentUiState()
}

class CreateTournamentViewModel(private val tournamentRepository: TournamentRepository) : ViewModel() {
    private val _state = MutableStateFlow<CreateTournamentUiState>(CreateTournamentUiState.Idle)
    val state: StateFlow<CreateTournamentUiState> = _state.asStateFlow()

    fun create(name: String, playerLimit: Int) {
        _state.value = CreateTournamentUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                CreateTournamentUiState.Created(tournamentRepository.create(name, playerLimit))
            } catch (e: ApiException) {
                CreateTournamentUiState.Error(e.message)
            }
        }
    }
}

@Composable
fun CreateTournamentScreen(onCreated: (Tournament) -> Unit, viewModel: CreateTournamentViewModel = koinViewModel()) {
    var name by remember { mutableStateOf("") }
    var playerLimit by remember { mutableStateOf("10") }
    val state by viewModel.state.collectAsState()

    (state as? CreateTournamentUiState.Created)?.let { onCreated(it.tournament) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("New tournament")
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = playerLimit,
            onValueChange = { playerLimit = it.filter(Char::isDigit) },
            label = { Text("Player limit (2-50)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { playerLimit.toIntOrNull()?.let { viewModel.create(name, it) } },
            enabled = state !is CreateTournamentUiState.Loading,
        ) { Text("Create") }
        if (state is CreateTournamentUiState.Loading) CircularProgressIndicator()
        if (state is CreateTournamentUiState.Error) Text((state as CreateTournamentUiState.Error).message)
    }
}
