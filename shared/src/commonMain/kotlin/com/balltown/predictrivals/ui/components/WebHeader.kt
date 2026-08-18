package com.balltown.predictrivals.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.balltown.predictrivals.data.repository.AuthRepository
import com.balltown.predictrivals.di.CurrentTournamentStore
import com.balltown.predictrivals.di.SessionStore
import com.balltown.predictrivals.res.Res
import com.balltown.predictrivals.res.app_logo
import com.balltown.predictrivals.res.logout_button
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Web-only top header: logo + brand name centered, logout pinned to the top-right corner.
 * Native platforms keep the Logout action on the Profile screen instead — see [isWebPlatform]. */
@Composable
fun WebHeader(onLoggedOut: () -> Unit) {
    val authRepository = koinInject<AuthRepository>()
    val sessionStore = koinInject<SessionStore>()
    val currentTournamentStore = koinInject<CurrentTournamentStore>()

    Box(
        modifier = Modifier.fillMaxWidth().height(64.dp).background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(painter = painterResource(Res.drawable.app_logo), contentDescription = null, modifier = Modifier.size(36.dp))
            Text("PREDICT RIVALS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        TextButton(
            onClick = {
                authRepository.logout()
                sessionStore.clear()
                currentTournamentStore.clear()
                onLoggedOut()
            },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) { Text(stringResource(Res.string.logout_button)) }
    }
}
