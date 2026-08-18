package com.balltown.predictrivals.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.balltown.predictrivals.res.Res
import com.balltown.predictrivals.res.field_password
import org.jetbrains.compose.resources.stringResource

@Composable
fun PasswordField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(Res.string.field_password)) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Text(if (visible) "🙈" else "👁")
            }
        },
        modifier = modifier,
    )
}
