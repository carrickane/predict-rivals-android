package com.balltown.predictrivals.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.balltown.predictrivals.di.CurrentTournamentStore
import com.balltown.predictrivals.res.Res
import com.balltown.predictrivals.res.action_cancel
import com.balltown.predictrivals.res.action_delete
import com.balltown.predictrivals.res.delete_account_button
import com.balltown.predictrivals.res.delete_account_dialog_text
import com.balltown.predictrivals.res.delete_account_dialog_title
import com.balltown.predictrivals.res.logout_button
import com.balltown.predictrivals.res.privacy_policy_button
import com.balltown.predictrivals.res.profile_stats_accuracy
import com.balltown.predictrivals.res.profile_stats_points
import com.balltown.predictrivals.res.profile_stats_predictions
import com.balltown.predictrivals.res.profile_title
import com.balltown.predictrivals.res.select_tournament_prompt
import com.balltown.predictrivals.res.send_feedback_button
import com.balltown.predictrivals.res.terms_button
import com.balltown.predictrivals.ui.components.RefreshableScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private const val FEEDBACK_EMAIL = "bstn.ref@gmail.com"

@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val currentTournamentStore = koinInject<CurrentTournamentStore>()
    val currentTournamentId by currentTournamentStore.currentTournamentId.collectAsState()
    val uriHandler = LocalUriHandler.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(currentTournamentId) { viewModel.load(currentTournamentId) }

    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val deleteAccountState by viewModel.deleteAccountState.collectAsState()

    LaunchedEffect(deleteAccountState) {
        if (deleteAccountState is DeleteAccountState.Deleted) onLoggedOut()
    }

    RefreshableScreen(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh(currentTournamentId) },
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.profile_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            when (val current = state) {
                is ProfileUiState.Loading -> CircularProgressIndicator()
                is ProfileUiState.Error -> Text(current.message)
                is ProfileUiState.Loaded -> {
                    val stats = current.stats
                    if (stats == null) {
                        Text(stringResource(Res.string.select_tournament_prompt))
                    } else {
                        Text(stringResource(Res.string.profile_stats_points, stats.totalPoints, stats.exactCount))
                        Text(stringResource(Res.string.profile_stats_predictions, stats.scoredPredictions, stats.totalPredictions))
                        Text(stringResource(Res.string.profile_stats_accuracy, stats.accuracy.toString()))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            OutlinedButton(onClick = onOpenPrivacyPolicy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.privacy_policy_button)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenTerms, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.terms_button)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { uriHandler.openUri("mailto:$FEEDBACK_EMAIL?subject=Predict%20Rivals%20feedback") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.send_feedback_button)) }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Button(onClick = { viewModel.logout(); onLoggedOut() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.logout_button)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(Res.string.delete_account_button)) }

            if (deleteAccountState is DeleteAccountState.Error) {
                Text((deleteAccountState as DeleteAccountState.Error).message, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(Res.string.delete_account_dialog_title)) },
            text = { Text(stringResource(Res.string.delete_account_dialog_text)) },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; viewModel.deleteAccount() },
                    enabled = deleteAccountState !is DeleteAccountState.Loading,
                ) { Text(stringResource(Res.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(Res.string.action_cancel)) } },
        )
    }
}
