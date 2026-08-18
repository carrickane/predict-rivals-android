package com.balltown.predictrivals.ui.tournament.live

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.balltown.predictrivals.res.Res
import com.balltown.predictrivals.res.round_score_row
import com.balltown.predictrivals.res.standings_heading
import com.balltown.predictrivals.res.standings_roundrobin_row
import com.balltown.predictrivals.res.standings_solo_row
import com.balltown.predictrivals.res.this_round_heading
import com.balltown.predictrivals.ui.components.FullScreenCenter
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LiveScreen(tournamentId: Int, viewModel: LiveViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.observe(tournamentId) }
    val state by viewModel.state.collectAsState()

    when (val current = state) {
        is LiveUiState.Loading -> FullScreenCenter { CircularProgressIndicator() }
        is LiveUiState.Error -> FullScreenCenter { Text(current.message) }
        is LiveUiState.Loaded -> {
            val snapshot = current.snapshot
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                snapshot.matches.forEach { match ->
                    Text("${match.homeTeam} ${match.homeScore ?: "-"} : ${match.awayScore ?: "-"} ${match.awayTeam} (${match.status})")
                }
                Text(stringResource(Res.string.standings_heading))
                snapshot.standings.forEach { standing ->
                    if (standing.isRoundRobin) {
                        val goalDiff = (standing.goalsFor ?: 0) - (standing.goalsAgainst ?: 0)
                        Text(
                            stringResource(
                                Res.string.standings_roundrobin_row,
                                standing.rank,
                                standing.name,
                                standing.leaguePoints ?: 0,
                                standing.wins ?: 0,
                                standing.draws ?: 0,
                                standing.losses ?: 0,
                                goalDiff,
                            ),
                        )
                    } else {
                        Text(stringResource(Res.string.standings_solo_row, standing.rank, standing.name, standing.totalPoints ?: 0))
                    }
                }
                if (snapshot.roundScores.isNotEmpty()) {
                    Text(stringResource(Res.string.this_round_heading))
                    snapshot.roundScores.forEach { entry ->
                        Text(stringResource(Res.string.round_score_row, entry.name, entry.roundPoints))
                    }
                }
            }
        }
    }
}
