package com.haoshuang_34517812.nutritrack.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.haoshuang_34517812.nutritrack.data.repository.AuthRepository
import com.haoshuang_34517812.nutritrack.data.repository.UsernameTakenException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val userId: String) : AuthUiState
    data object ResetEmailSent : AuthUiState
    data class Error(val message: String) : AuthUiState
}

/**
 * Drives the email/password login and registration screens on top of
 * [AuthRepository]. All validation messages are user-facing strings.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your email and password")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            authRepository.login(trimmedEmail, password)
                .onSuccess { user -> _uiState.value = AuthUiState.Success(user.uid) }
                .onFailure { e -> _uiState.value = AuthUiState.Error(friendlyMessage(e)) }
        }
    }

    fun register(username: String, email: String, password: String, confirmPassword: String) {
        val trimmedUsername = username.trim()
        val trimmedEmail = email.trim()
        when {
            trimmedUsername.isBlank() ->
                return fail("Please choose a username")
            !USERNAME_REGEX.matches(trimmedUsername) ->
                return fail("Username must be 3-20 characters: letters, numbers or underscore")
            trimmedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() ->
                return fail("Please enter a valid email address")
            password.length < 6 ->
                return fail("Password must be at least 6 characters")
            password != confirmPassword ->
                return fail("Passwords do not match")
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            authRepository.register(trimmedUsername, trimmedEmail, password)
                .onSuccess { user -> _uiState.value = AuthUiState.Success(user.uid) }
                .onFailure { e -> _uiState.value = AuthUiState.Error(friendlyMessage(e)) }
        }
    }

    fun sendPasswordReset(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.value = AuthUiState.Error("Enter your account email above, then tap Forgot password")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            authRepository.sendPasswordReset(trimmedEmail)
                .onSuccess { _uiState.value = AuthUiState.ResetEmailSent }
                .onFailure { e -> _uiState.value = AuthUiState.Error(friendlyMessage(e)) }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun fail(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }

    private fun friendlyMessage(e: Throwable): String = when (e) {
        is UsernameTakenException -> "This username is already taken"
        is FirebaseAuthUserCollisionException -> "An account with this email already exists"
        is FirebaseAuthWeakPasswordException -> "Password is too weak - use at least 6 characters"
        is FirebaseAuthInvalidUserException -> "No account found for this email"
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password"
        is FirebaseNetworkException -> "Network error - check your connection and try again"
        else -> e.localizedMessage ?: "Something went wrong. Please try again"
    }

    companion object {
        private val USERNAME_REGEX = Regex("^[A-Za-z0-9_]{3,20}$")
    }
}
