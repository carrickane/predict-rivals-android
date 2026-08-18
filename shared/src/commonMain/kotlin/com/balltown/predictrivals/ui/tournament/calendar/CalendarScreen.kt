package com.balltown.predictrivals.ui.tournament.calendar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.balltown.predictrivals.res.Res
import com.balltown.predictrivals.res.match_vs
import com.balltown.predictrivals.res.no_rounds_yet
import com.balltown.predictrivals.res.opponent_bot
import com.balltown.predictrivals.res.opponent_unknown
import com.balltown.predictrivals.res.round_opponent
import com.balltown.predictrivals.res.round_status
import com.balltown.predictrivals.ui.components.FullScreenCenter
import com.balltown.predictrivals.ui.components.RefreshableScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CalendarScreen(tournamentId: Int, viewModel: CalendarViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.load(tournamentId) }
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    RefreshableScreen(isRefreshing = isRefreshing, onRefresh = { viewModel.refresh(tournamentId) }) {
        when (val current = state) {
            is CalendarUiState.Loading -> FullScreenCenter { CircularProgressIndicator() }
            is CalendarUiState.Error -> FullScreenCenter { Text(current.message) }
            is CalendarUiState.Loaded -> if (current.rounds.isEmpty()) {
                FullScreenCenter { Text(stringResource(Res.string.no_rounds_yet)) }
            } else {
                val botLabel = stringResource(Res.string.opponent_bot)
                val unknownLabel = stringResource(Res.string.opponent_unknown)
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    current.rounds.forEach { round ->
                        val opponent = current.opponentsByRound[round.roundNumber]
                        if (opponent != null) {
                            item {
                                Text(
                                    text = stringResource(
                                        Res.string.round_opponent,
                                        round.roundNumber,
                                        if (opponent.isBotMatch) botLabel else (opponent.opponentName ?: unknownLabel),
                                    ),
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                        }
                        items(round.matches) { match ->
                            ListItem(
                                headlineContent = { Text(stringResource(Res.string.match_vs, match.homeTeam, match.awayTeam)) },
                                supportingContent = { Text(stringResource(Res.string.round_status, round.roundNumber, match.status)) },
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
