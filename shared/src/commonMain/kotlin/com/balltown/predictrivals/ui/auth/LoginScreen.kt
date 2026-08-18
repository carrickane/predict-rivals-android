package com.balltown.predictrivals.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.balltown.predictrivals.res.Res
import com.balltown.predictrivals.res.field_email
import com.balltown.predictrivals.res.login_button
import com.balltown.predictrivals.res.login_register_prompt
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onNavigateToRegister: () -> Unit, viewModel: AuthViewModel = koinViewModel()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    if (state is AuthUiState.Success) onLoggedIn()

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Predict Rivals")
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(Res.string.field_email)) },
            modifier = Modifier.fillMaxWidth(),
        )
        PasswordField(value = password, onValueChange = { password = it })
        Button(onClick = { viewModel.login(email, password) }, modifier = Modifier.fillMaxWidth(), enabled = state !is AuthUiState.Loading) {
            Text(stringResource(Res.string.login_button))
        }
        TextButton(onClick = onNavigateToRegister) { Text(stringResource(Res.string.login_register_prompt)) }
        if (state is AuthUiState.Loading) CircularProgressIndicator()
        if (state is AuthUiState.Error) Text((state as AuthUiState.Error).message)
    }
}
