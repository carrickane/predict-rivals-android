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
import com.balltown.predictrivals.res.field_name
import com.balltown.predictrivals.res.register_button
import com.balltown.predictrivals.res.register_login_prompt
import com.balltown.predictrivals.res.register_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RegisterScreen(onRegistered: () -> Unit, onNavigateToLogin: () -> Unit, viewModel: AuthViewModel = koinViewModel()) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    if (state is AuthUiState.Success) onRegistered()

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.register_title))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(Res.string.field_name)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(Res.string.field_email)) },
            modifier = Modifier.fillMaxWidth(),
        )
        PasswordField(value = password, onValueChange = { password = it })
        Button(onClick = { viewModel.register(email, password, name) }, modifier = Modifier.fillMaxWidth(), enabled = state !is AuthUiState.Loading) {
            Text(stringResource(Res.string.register_button))
        }
        TextButton(onClick = onNavigateToLogin) { Text(stringResource(Res.string.register_login_prompt)) }
        if (state is AuthUiState.Loading) CircularProgressIndicator()
        if (state is AuthUiState.Error) Text((state as AuthUiState.Error).message)
    }
}
