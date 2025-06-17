package ru.ridecorder.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.ridecorder.R

@Composable
fun LoginScreen(viewModel: AuthViewModel = hiltViewModel(), onBackClick: () -> Unit, onSuccess: () -> Unit) {

    val authState by viewModel.authState.collectAsState()

    val emailState = remember { mutableStateOf("") }
    val passwordState = remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Цвет фона
            .imePadding()
    ) {
        // Крестик в правом верхнем углу
        FilledIconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 30.dp)
                .padding(horizontal = 20.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), // Серый круг
                contentColor = MaterialTheme.colorScheme.onSecondary   // Белая иконка
            )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(id = R.string.login_close)
            )
        }

        // Основной контент экрана по центру
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Заголовок "Войдите в Ridecorder"
            Text(
                text = stringResource(id = R.string.login_screen_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Поле Email
            OutlinedTextField(
                value = emailState.value,
                onValueChange = { emailState.value = it },
                label = { Text(stringResource(id = R.string.email)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Поле Password
            OutlinedTextField(
                value = passwordState.value,
                onValueChange = { passwordState.value = it },
                label = { Text(stringResource(id = R.string.password)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Ссылка "Забыли пароль?"
            Text(
                text = stringResource(id = R.string.forgot_password),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                ),
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable {
                        // Обработка нажатия
                    }
            )

            Spacer(modifier = Modifier.height(12.dp))

            AuthStateScreen(authState, onSuccess)

            Spacer(modifier = Modifier.height(12.dp))

            // Кнопка "Войти"
            Button(
                onClick = {
                    viewModel.login(emailState.value, passwordState.value)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = stringResource(id = R.string.login_button),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun AuthStateScreen(authState: AuthState, onSuccess: () -> Unit) {
    when (authState) {
        is AuthState.Idle -> Unit // Ничего не делаем по умолчанию
        is AuthState.Loading -> {
            CircularProgressIndicator()
        }
        is AuthState.Success -> {
            val response = (authState as AuthState.Success).authResponse
            Text(text = stringResource(id = R.string.auth_success), color = Color.Green)
            LaunchedEffect(Unit) {
                onSuccess()
            }
        }
        is AuthState.Error -> {
            val errorMessage = (authState as AuthState.Error).message
            Text(text = stringResource(id = R.string.auth_error, errorMessage), color = Color.Red)
        }
    }
}

