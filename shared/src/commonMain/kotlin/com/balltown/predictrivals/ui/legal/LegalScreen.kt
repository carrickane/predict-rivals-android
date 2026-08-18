package com.balltown.predictrivals.ui.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Read-only viewer for a legal document, reached from the Profile screen. */
@Composable
fun LegalScreen(title: String, body: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Column(modifier = Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 16.dp)) {
            Text(body)
        }
        TextButton(onClick = onBack) { Text("Back") }
    }
}

/**
 * Mandatory first-launch gate: nothing else in the app is reachable until the user accepts.
 * Also reachable indirectly any time via the Profile screen's Privacy Policy / Terms links,
 * which use [LegalScreen] instead since they don't need the accept/decline affordance.
 */
@Composable
fun TermsGateScreen(onAccept: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Before you continue", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            "Please review our $PRIVACY_POLICY_TITLE and $TERMS_TITLE. Continuing means you accept both.",
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            textAlign = TextAlign.Center,
        )
        Column(modifier = Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(TERMS_TITLE, style = MaterialTheme.typography.titleMedium)
            Text(TERMS_TEXT, modifier = Modifier.padding(bottom = 16.dp))
            Text(PRIVACY_POLICY_TITLE, style = MaterialTheme.typography.titleMedium)
            Text(PRIVACY_POLICY_TEXT)
        }
        Button(onClick = onAccept, modifier = Modifier.padding(top = 24.dp)) { Text("Accept & continue") }
    }
}
