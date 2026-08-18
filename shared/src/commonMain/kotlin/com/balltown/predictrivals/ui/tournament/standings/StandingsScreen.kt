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
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StandingsScreen(tournamentId: Int, viewModel: StandingsViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.load(tournamentId) }
    val state by viewModel.state.collectAsState()

    when (val current = state) {
        is StandingsUiState.Loading -> CircularProgressIndicator()
        is StandingsUiState.Error -> Text(current.message)
        is StandingsUiState.Loaded -> Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row {
                Text("Top scorers")
                Switch(checked = current.showingTopScorers, onCheckedChange = { viewModel.load(tournamentId, topScorers = it) })
            }
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
    }
}
