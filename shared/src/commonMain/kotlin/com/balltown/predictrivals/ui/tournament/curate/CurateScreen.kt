package com.balltown.predictrivals.ui.tournament.curate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.balltown.predictrivals.ui.components.FullScreenCenter
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CurateScreen(tournamentId: Int, from: String, to: String, onRoundCreated: () -> Unit, viewModel: CurateViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.search(from, to) }
    val state by viewModel.state.collectAsState()

    if (state is CurateUiState.RoundCreated) onRoundCreated()

    when (val current = state) {
        is CurateUiState.Idle, is CurateUiState.Loading -> FullScreenCenter { CircularProgressIndicator() }
        is CurateUiState.Error -> FullScreenCenter { Text(current.message) }
        is CurateUiState.RoundCreated -> FullScreenCenter { Text("Round created.") }
        is CurateUiState.CandidatesLoaded -> Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Pick exactly 9 fixtures (${current.selected.size}/9)")
            if (current.candidates.isEmpty()) {
                Text("No fixtures available for this date range yet.")
            }
            LazyColumn {
                items(current.candidates) { fixture ->
                    Row(modifier = Modifier.clickable { viewModel.toggleSelected(fixture.id) }.padding(vertical = 4.dp)) {
                        Checkbox(checked = fixture.id in current.selected, onCheckedChange = { viewModel.toggleSelected(fixture.id) })
                        Text("${fixture.homeTeam} vs ${fixture.awayTeam} (${fixture.competition})")
                    }
                }
            }
            Button(onClick = { viewModel.createRound(tournamentId) }, enabled = current.selected.size == 9) {
                Text("Create round")
            }
        }
    }
}
