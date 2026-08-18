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
import com.balltown.predictrivals.res.Res
import com.balltown.predictrivals.res.create_round_button
import com.balltown.predictrivals.res.fixture_row
import com.balltown.predictrivals.res.no_fixtures_available
import com.balltown.predictrivals.res.pick_fixtures_title
import com.balltown.predictrivals.res.round_created_message
import com.balltown.predictrivals.ui.components.FullScreenCenter
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CurateScreen(tournamentId: Int, from: String, to: String, onRoundCreated: () -> Unit, viewModel: CurateViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.search(from, to) }
    val state by viewModel.state.collectAsState()

    if (state is CurateUiState.RoundCreated) onRoundCreated()

    when (val current = state) {
        is CurateUiState.Idle, is CurateUiState.Loading -> FullScreenCenter { CircularProgressIndicator() }
        is CurateUiState.Error -> FullScreenCenter { Text(current.message) }
        is CurateUiState.RoundCreated -> FullScreenCenter { Text(stringResource(Res.string.round_created_message)) }
        is CurateUiState.CandidatesLoaded -> Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(stringResource(Res.string.pick_fixtures_title, current.selected.size))
            if (current.candidates.isEmpty()) {
                Text(stringResource(Res.string.no_fixtures_available))
            }
            LazyColumn {
                items(current.candidates) { fixture ->
                    Row(modifier = Modifier.clickable { viewModel.toggleSelected(fixture.id) }.padding(vertical = 4.dp)) {
                        Checkbox(checked = fixture.id in current.selected, onCheckedChange = { viewModel.toggleSelected(fixture.id) })
                        Text(stringResource(Res.string.fixture_row, fixture.homeTeam, fixture.awayTeam, fixture.competition))
                    }
                }
            }
            Button(onClick = { viewModel.createRound(tournamentId) }, enabled = current.selected.size == 9) {
                Text(stringResource(Res.string.create_round_button))
            }
        }
    }
}
