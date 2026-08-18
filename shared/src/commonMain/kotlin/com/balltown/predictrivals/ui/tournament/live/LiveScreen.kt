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
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LiveScreen(tournamentId: Int, viewModel: LiveViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.observe(tournamentId) }
    val snapshot by viewModel.snapshot.collectAsState()

    val current = snapshot
    if (current == null) {
        CircularProgressIndicator()
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            current.matches.forEach { match ->
                Text("${match.homeTeam} ${match.homeScore ?: "-"} : ${match.awayScore ?: "-"} ${match.awayTeam} (${match.status})")
            }
            Text("Standings")
            current.standings.forEach { standing -> Text("#${standing.rank} ${standing.name}: ${standing.totalPoints}") }
        }
    }
}
