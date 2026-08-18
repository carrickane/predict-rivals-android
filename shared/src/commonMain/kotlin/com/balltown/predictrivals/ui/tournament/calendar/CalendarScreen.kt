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
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CalendarScreen(tournamentId: Int, viewModel: CalendarViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.load(tournamentId) }
    val state by viewModel.state.collectAsState()

    when (val current = state) {
        is CalendarUiState.Loading -> CircularProgressIndicator()
        is CalendarUiState.Error -> Text(current.message)
        is CalendarUiState.Loaded -> if (current.rounds.isEmpty()) {
            Text("No rounds yet.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                current.rounds.forEach { round ->
                    items(round.matches) { match ->
                        ListItem(
                            headlineContent = { Text("${match.homeTeam} vs ${match.awayTeam}") },
                            supportingContent = { Text("Round ${round.roundNumber} · ${match.status}") },
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
