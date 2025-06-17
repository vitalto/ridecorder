package ru.ridecorder.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import ru.ridecorder.R
import ru.ridecorder.config.AppConfig
import ru.ridecorder.data.remote.network.models.ApiResponse
import ru.ridecorder.ui.common.LoadingScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    viewModel: ProfileEditViewModel = hiltViewModel(),
    onBack: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val isLoading = viewModel.isLoading.collectAsState()

    var name by remember { mutableStateOf(viewModel.name) }
    var username by remember { mutableStateOf(viewModel.username) }
    var avatarUri by remember { mutableStateOf(viewModel.avatarUri) }
    // Состояние для ввода веса (в кг), не обязательно
    var weight by remember { mutableStateOf(viewModel.weight) }
    // Состояние для выбора пола
    var gender by remember { mutableStateOf(viewModel.gender) }
    // Состояние для выбора публичного или приватного профиля
    var isProfilePrivate by remember { mutableStateOf(viewModel.isProfilePrivate) }

    val updateResult: ApiResponse<Boolean>? = viewModel.updateResult
    var isSaving by remember { mutableStateOf(false) }

    // Загрузка данных из ViewModel (при первом запуске)
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }
    LaunchedEffect(viewModel.name, viewModel.username, viewModel.avatarUri, viewModel.weight, viewModel.gender, viewModel.isProfilePrivate) {
        name = viewModel.name
        username = viewModel.username
        avatarUri = viewModel.avatarUri
        weight = viewModel.weight
        gender = viewModel.gender
        isProfilePrivate = viewModel.isProfilePrivate
    }

    // Лаунчер для выбора картинки
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                viewModel.onAvatarSelected(uri)
                avatarUri = uri
            }
        }
    )

    if (isLoading.value) {
        LoadingScreen()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.profile_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = { onBack(false) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Аватар
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = avatarUri ?: "${AppConfig.serverUrl}/avatars/placeholder.jpg",
                    contentDescription = stringResource(id = R.string.avatar),
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { pickImageLauncher.launch("image/*") }) {
                        Text(text = stringResource(id = R.string.change_avatar))
                    }
                    if (avatarUri != null && !avatarUri.toString().endsWith("placeholder.jpg")) {
                        Button(onClick = { viewModel.onAvatarDeleted() }) {
                            Text(text = stringResource(id = R.string.delete_avatar))
                        }
                    }
                }
            }

            // Поле для имени
            OutlinedTextField(
                value = name ?: "",
                onValueChange = { viewModel.onNameChange(it); name = it },
                label = { Text(stringResource(id = R.string.name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Поле для логина
            OutlinedTextField(
                value = username ?: "",
                onValueChange = { viewModel.onUsernameChange(it); username = it },
                label = { Text(stringResource(id = R.string.username)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Поле для ввода веса (в кг) - не обязательно
            OutlinedTextField(
                value = (weight ?: 0.0).toString(),
                onValueChange = { viewModel.onWeightChange(it); weight = it.toFloatOrNull() ?: 0f },
                label = { Text(stringResource(id = R.string.weight_kg)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Выбор пола
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(id = R.string.gender))
                Spacer(modifier = Modifier.width(8.dp))
                RadioButton(
                    selected = gender == "male",
                    onClick = { viewModel.onGenderChange("male"); gender = "male" }
                )
                Text(text = stringResource(id = R.string.male))
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = gender == "female",
                    onClick = { viewModel.onGenderChange("female"); gender = "female" }
                )
                Text(text = stringResource(id = R.string.female))
            }

            // Выбор типа профиля: публичный или приватный
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(id = R.string.private_profile))
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = isProfilePrivate ?: false,
                    onCheckedChange = {
                        viewModel.onProfilePrivateChange(it)
                        isProfilePrivate = it
                    }
                )
            }

            // Кнопка "Сохранить"
            Button(
                onClick = {
                    isSaving = true
                    viewModel.updateProfile(context) { isSuccess ->
                        isSaving = false
                        if (isSuccess)
                            onBack(true) // Вернуться в профиль, если успешно
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(stringResource(id = R.string.save))
                }
            }

            // Результат обновления
            updateResult?.let {
                Text(
                    text = if (it.success) stringResource(id = R.string.profile_updated_success)
                    else stringResource(id = R.string.profile_update_error, it.errorMessage.orEmpty()),
                    color = if (it.success) Color.Green else Color.Red
                )
            }
        }
    }
}
