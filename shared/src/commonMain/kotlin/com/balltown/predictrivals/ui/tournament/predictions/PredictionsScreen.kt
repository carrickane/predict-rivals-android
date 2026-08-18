package com.balltown.predictrivals.ui.tournament.predictions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.balltown.predictrivals.domain.model.Match
import com.balltown.predictrivals.domain.scoring.isPredictionEditable
import com.balltown.predictrivals.res.Res
import com.balltown.predictrivals.res.action_save
import com.balltown.predictrivals.res.match_vs
import com.balltown.predictrivals.res.no_round_setup_yet
import com.balltown.predictrivals.ui.components.FullScreenCenter
import com.balltown.predictrivals.ui.components.RefreshableScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
fun PredictionsScreen(tournamentId: Int, viewModel: PredictionsViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.load(tournamentId) }
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    RefreshableScreen(isRefreshing = isRefreshing, onRefresh = { viewModel.refresh(tournamentId) }) {
        when (val current = state) {
            is PredictionsUiState.Loading -> FullScreenCenter { CircularProgressIndicator() }
            is PredictionsUiState.Error -> FullScreenCenter { Text(current.message) }
            is PredictionsUiState.Loaded -> if (current.round == null) {
                FullScreenCenter { Text(stringResource(Res.string.no_round_setup_yet)) }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    current.round.matches.forEach { match ->
                        MatchPredictionRow(match, onSubmit = { home, away -> viewModel.submit(match.id, home, away) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun MatchPredictionRow(match: Match, onSubmit: (Int, Int) -> Unit) {
    var home by remember { mutableStateOf("") }
    var away by remember { mutableStateOf("") }
    val editable = isPredictionEditable(kickoffAt = Instant.parse(match.kickoffAt))

    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(stringResource(Res.string.match_vs, match.homeTeam, match.awayTeam), modifier = Modifier.padding(end = 8.dp))
        OutlinedTextField(
            value = home,
            onValueChange = { home = it.filter(Char::isDigit) },
            enabled = editable,
            modifier = Modifier.padding(end = 4.dp),
        )
        OutlinedTextField(value = away, onValueChange = { away = it.filter(Char::isDigit) }, enabled = editable)
        Button(
            onClick = { home.toIntOrNull()?.let { h -> away.toIntOrNull()?.let { a -> onSubmit(h, a) } } },
            enabled = editable,
        ) { Text(stringResource(Res.string.action_save)) }
    }
}
