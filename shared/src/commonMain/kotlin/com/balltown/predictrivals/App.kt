package com.balltown.predictrivals

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.balltown.predictrivals.data.storage.AppPreferences
import com.balltown.predictrivals.di.appModule
import com.balltown.predictrivals.di.sessionModule
import com.balltown.predictrivals.di.viewModelModule
import com.balltown.predictrivals.ui.legal.TermsGateScreen
import com.balltown.predictrivals.ui.navigation.NavGraph
import com.balltown.predictrivals.ui.theme.PredictRivalsTheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

@Composable
fun App() {
    KoinApplication(application = { modules(appModule, sessionModule, viewModelModule) }) {
        PredictRivalsTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                val appPreferences = koinInject<AppPreferences>()
                var termsAccepted by remember { mutableStateOf(appPreferences.hasAcceptedTerms) }

                if (termsAccepted) {
                    NavGraph()
                } else {
                    TermsGateScreen(onAccept = {
                        appPreferences.hasAcceptedTerms = true
                        termsAccepted = true
                    })
                }
            }
        }
    }
}