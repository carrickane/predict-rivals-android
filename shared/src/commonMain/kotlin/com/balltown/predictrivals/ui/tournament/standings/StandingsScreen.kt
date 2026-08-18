package com.balltown.predictrivals.ui.tournament.standings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
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
private fun TopScorersToggle(checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row {
        Text("Top scorers")
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
fun StandingsScreen(tournamentId: Int, viewModel: StandingsViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.load(tournamentId) }
    val state by viewModel.state.collectAsState()

    when (val current = state) {
        is StandingsUiState.Loading -> FullScreenCenter { CircularProgressIndicator() }
        is StandingsUiState.Error -> FullScreenCenter { Text(current.message) }

        is StandingsUiState.LoadedSolo -> Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            TopScorersToggle(current.showingTopScorers) { viewModel.load(tournamentId, topScorers = it) }
            LazyColumn {
                items(current.standings) { standing ->
                    ListItem(
                        headlineContent = { Text("#${standing.rank} ${standing.name}") },
                        supportingContent = { Text("${standing.totalPoints} pts · ${standing.exactCount} exact") },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }

        is StandingsUiState.LoadedRoundRobinStandings -> Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            TopScorersToggle(checked = false) { viewModel.load(tournamentId, topScorers = it) }
            LazyColumn {
                items(current.standings) { row ->
                    ListItem(
                        headlineContent = { Text("#${row.rank} ${row.name}") },
                        supportingContent = {
                            Text("${row.leaguePoints} pts · ${row.wins}W ${row.draws}D ${row.losses}L · GF ${row.goalsFor} GA ${row.goalsAgainst} (GD ${row.goalDifference})")
                        },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }

        is StandingsUiState.LoadedRoundRobinTopScorers -> Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            TopScorersToggle(checked = true) { viewModel.load(tournamentId, topScorers = it) }
            LazyColumn {
                items(current.topScorers) { row ->
                    ListItem(
                        headlineContent = { Text("#${row.rank} ${row.name}") },
                        supportingContent = { Text("${row.goalsFor} goals") },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}
