package com.songshawn.nutritrack.viewmodel

import com.songshawn.nutritrack.data.network.fruityvice.FruitDto
import java.io.IOException

sealed interface LoginState {
    data object Initial : LoginState
    data class Authenticating(val isLoading: Boolean) : LoginState
    data object NeedsRegistration : LoginState
    data class Success(val userId: String) : LoginState
    data class Error(val message: String) : LoginState
}

sealed interface RegistrationState {
    data object Initial : RegistrationState
    data object VerifyingPhone : RegistrationState
    data object PhoneVerified : RegistrationState
    data object Submitting : RegistrationState
    data class Success(val userId: String) : RegistrationState
    data class Error(val message: String) : RegistrationState
}

sealed interface FieldValidationState {
    data object NotValidated : FieldValidationState
    data object Valid : FieldValidationState
    data object Empty : FieldValidationState
    data class Invalid(val message: String) : FieldValidationState
}

data class RegisterFormState(
    val phoneNumber: String = "",
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val phoneValidation: FieldValidationState = FieldValidationState.NotValidated,
    val usernameValidation: FieldValidationState = FieldValidationState.NotValidated,
    val passwordValidation: FieldValidationState = FieldValidationState.NotValidated,
    val confirmPasswordValidation: FieldValidationState = FieldValidationState.NotValidated,
    val passwordVisible: Boolean = false,
)

sealed interface SettingsUiState {
    data object Initial : SettingsUiState
    data object Loading : SettingsUiState
    data object Success : SettingsUiState
    data object Logout : SettingsUiState
    data object AccountDeleted : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

sealed interface ApiResult<out T> {
    data object Initial : ApiResult<Nothing>
    data object Loading : ApiResult<Nothing>
    data class Success<T>(val data: T) : ApiResult<T>

    sealed interface Error : ApiResult<Nothing> {
        data class Network(val cause: IOException) : Error
        data class Http(val code: Int, val message: String, val body: String?) : Error
        data class Parsing(val cause: Throwable) : Error
        data class Unknown(val cause: Throwable) : Error
    }
}
sealed interface FruitUiState {
    data object Initial : FruitUiState
    data object Loading : FruitUiState
    data class Success(val fruit: FruitDto) : FruitUiState
    data class Error(val message: String) : FruitUiState
}

sealed class GenAiUiState {
    data object Idle : GenAiUiState()
    data class Loading(val message: String = "Loading...") : GenAiUiState()
    data class Content(val text: String) : GenAiUiState()
    data class Streaming(val text: String, val isComplete: Boolean = false) : GenAiUiState()  // 流式输出状态
    data class Error(val message: String) : GenAiUiState()           // 远端错误展示文案
    data class Validation(val message: String) : GenAiUiState()      // ✅ 本地校验
}