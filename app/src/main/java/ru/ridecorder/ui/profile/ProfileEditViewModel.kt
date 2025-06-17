package ru.ridecorder.ui.profile

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.ridecorder.R
import ru.ridecorder.config.AppConfig
import ru.ridecorder.data.remote.network.models.ApiResponse
import ru.ridecorder.data.repository.ProfileRepository
import ru.ridecorder.di.ResourceProvider
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    // Состояния, которые будет редактировать пользователь
    var name by mutableStateOf<String?>(null)
        private set

    var username by mutableStateOf<String?>(null)
        private set

    var avatarUri by mutableStateOf<Uri?>(null)
        private set

    var weight by mutableStateOf<Float?>(null)
        private set

    var gender by mutableStateOf<String?>(null)
        private set

    var isProfilePrivate by mutableStateOf<Boolean?>(null)
        private set

    // Статус/результат операции
    var updateResult by mutableStateOf<ApiResponse<Boolean>?>(null)
        private set


    fun loadProfile() {
        viewModelScope.launch {
            try {
                val response = profileRepository.getProfile()
                if (response.success) {
                    val user = response.data
                    name = user?.fullName ?: ""
                    username = user?.username ?: ""
                    if(user?.avatarUrl != null){
                        avatarUri = Uri.parse(user.avatarUrl)
                    }
                    weight = user?.weight
                    gender = user?.gender
                    isProfilePrivate = user?.isPrivate
                } else {
                    updateResult = ApiResponse(resourceProvider.getString(R.string.profile_load_error))
                }
            } catch (e: Exception) {
                updateResult = ApiResponse(resourceProvider.getString(R.string.profile_load_error))
            }
            finally {
                _isLoading.value = false
            }
        }
    }

    fun onNameChange(newName: String) {
        name = newName
    }

    fun onUsernameChange(newUsername: String) {
        username = newUsername
    }
    fun onWeightChange(newWeight: String) {
        weight = newWeight.toFloatOrNull() ?: 0f
    }
    fun onGenderChange(newGender: String) {
        gender = newGender
    }
    fun onProfilePrivateChange(newIsPrivate: Boolean) {
        isProfilePrivate = newIsPrivate
    }

    fun onAvatarSelected(uri: Uri?) {
        avatarUri = uri
    }
    fun onAvatarDeleted() {
        viewModelScope.launch {
            try {
                profileRepository.deleteAvatar()
                avatarUri = Uri.parse(AppConfig.serverUrl + "/avatars/placeholder.jpg")
            }catch (e: Exception){
                e.printStackTrace()
                updateResult = ApiResponse(resourceProvider.getString(R.string.avatar_delete_error))
            }
        }
    }
    fun updateProfile(context: Context, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // 1. Формируем RequestBody для текстовых полей
                val nameBody = name
                    ?.toRequestBody("text/plain".toMediaTypeOrNull())
                val usernameBody = username
                    ?.toRequestBody("text/plain".toMediaTypeOrNull())

                // 2. Готовим MultipartBody.Part для аватара, если пользователь выбрал новый
                val avatarPart = if (avatarUri != null && avatarUri?.scheme != "https") {
                    prepareFilePart(context, avatarUri!!)
                } else {
                    null
                }


                val weightBody = weight?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

                val genderBody = gender?.toRequestBody("text/plain".toMediaTypeOrNull())


                val isProfilePrivateBody = isProfilePrivate?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())


                // 3. Делаем запрос на сервер
                val response = profileRepository.updateProfile(
                    fullName = nameBody,
                    username = usernameBody,
                    avatar = avatarPart,
                    weight = weightBody,
                    gender = genderBody,
                    isPrivate = isProfilePrivateBody,
                )
                updateResult = response

                // Если нужно обновить локальные поля после ответа
                if (response.success) {
                    onResult(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateResult = ApiResponse(resourceProvider.getString(R.string.internet_issue))
            }
            onResult(false)
        }
    }

    private fun prepareFilePart(context: Context, uri: Uri): MultipartBody.Part {
        val contentResolver = context.contentResolver

        // Получаем MIME-тип файла
        val mimeType = contentResolver.getType(uri) ?: "image/jpeg" // По умолчанию ставим JPEG

        // Получаем расширение файла
        val fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            ?: "jpg"

        // Открываем поток и создаём временный файл с нужным расширением
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Невозможно открыть файл по Uri = $uri")

        val tempFile = File.createTempFile("avatar_temp", ".$fileExtension", context.cacheDir)
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }

        // Создаём RequestBody
        val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())

        return MultipartBody.Part.createFormData(
            name = "avatar", // ключ, который сервер ждёт для файла
            filename = "avatar.$fileExtension", // Используем корректное расширение
            body = requestBody
        )
    }


}
