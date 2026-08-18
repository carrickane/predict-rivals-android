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
    val deleteAccountState by viewModel.deleteAccountState.collectAsState()

    LaunchedEffect(deleteAccountState) {
        if (deleteAccountState is DeleteAccountState.Deleted) onLoggedOut()
    }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        when (val current = state) {
            is ProfileUiState.Loading -> CircularProgressIndicator()
            is ProfileUiState.Error -> Text(current.message)
            is ProfileUiState.Loaded -> {
                val stats = current.stats
                if (stats == null) {
                    Text("Select a tournament to see your stats here.")
                } else {
                    Text("${stats.totalPoints} points · ${stats.exactCount} exact")
                    Text("${stats.scoredPredictions}/${stats.totalPredictions} predictions scored")
                    Text("Accuracy: ${stats.accuracy}")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        OutlinedButton(onClick = onOpenPrivacyPolicy, modifier = Modifier.fillMaxWidth()) { Text("Privacy Policy") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onOpenTerms, modifier = Modifier.fillMaxWidth()) { Text("Terms & Conditions") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { uriHandler.openUri("mailto:$FEEDBACK_EMAIL?subject=Predict%20Rivals%20feedback") },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Send feedback") }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        Button(onClick = { viewModel.logout(); onLoggedOut() }, modifier = Modifier.fillMaxWidth()) { Text("Log out") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) { Text("Delete account") }

        if (deleteAccountState is DeleteAccountState.Error) {
            Text((deleteAccountState as DeleteAccountState.Error).message, color = MaterialTheme.colorScheme.error)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete account?") },
            text = { Text("This permanently deletes your account, tournaments you own, and your prediction history. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; viewModel.deleteAccount() },
                    enabled = deleteAccountState !is DeleteAccountState.Loading,
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}
