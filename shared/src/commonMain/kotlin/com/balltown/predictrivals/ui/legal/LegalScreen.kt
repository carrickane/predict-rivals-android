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
import com.balltown.predictrivals.res.Res
import com.balltown.predictrivals.res.accept_continue_button
import com.balltown.predictrivals.res.action_back
import com.balltown.predictrivals.res.terms_gate_heading
import com.balltown.predictrivals.res.terms_gate_intro
import org.jetbrains.compose.resources.stringResource

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
        TextButton(onClick = onBack) { Text(stringResource(Res.string.action_back)) }
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
        Text(stringResource(Res.string.terms_gate_heading), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            stringResource(Res.string.terms_gate_intro, PRIVACY_POLICY_TITLE, TERMS_TITLE),
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            textAlign = TextAlign.Center,
        )
        Column(modifier = Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(TERMS_TITLE, style = MaterialTheme.typography.titleMedium)
            Text(TERMS_TEXT, modifier = Modifier.padding(bottom = 16.dp))
            Text(PRIVACY_POLICY_TITLE, style = MaterialTheme.typography.titleMedium)
            Text(PRIVACY_POLICY_TEXT)
        }
        Button(onClick = onAccept, modifier = Modifier.padding(top = 24.dp)) { Text(stringResource(Res.string.accept_continue_button)) }
    }
}
