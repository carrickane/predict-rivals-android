package com.balltown.predictrivals.ui.tournament

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.balltown.predictrivals.di.CurrentTournamentStore
import com.balltown.predictrivals.res.Res
import com.balltown.predictrivals.res.create_or_join_prompt
import com.balltown.predictrivals.res.go_to_tournament_button
import com.balltown.predictrivals.res.no_tournament_selected
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Gate for the Calendar/Live/Standings tabs, which act on "the current tournament" rather than
 * one passed via nav args. Shows a pick-a-tournament prompt when nothing is selected yet.
 */
@Composable
fun RequiresCurrentTournament(onGoToTournamentTab: () -> Unit, content: @Composable (tournamentId: Int) -> Unit) {
    val currentTournamentStore = koinInject<CurrentTournamentStore>()
    val tournamentId by currentTournamentStore.currentTournamentId.collectAsState()

    val id = tournamentId
    if (id == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(Res.string.no_tournament_selected),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(Res.string.create_or_join_prompt),
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                textAlign = TextAlign.Center,
            )
            Button(onClick = onGoToTournamentTab) { Text(stringResource(Res.string.go_to_tournament_button)) }
        }
    } else {
        content(id)
    }
}
