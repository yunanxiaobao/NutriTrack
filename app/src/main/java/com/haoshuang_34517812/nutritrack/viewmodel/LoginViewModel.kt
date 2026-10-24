package com.haoshuang_34517812.nutritrack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshuang_34517812.nutritrack.data.repository.PatientRepository
import com.haoshuang_34517812.nutritrack.util.AuthenticationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineExceptionHandler
import java.security.MessageDigest
import javax.inject.Inject

/**
 * ViewModel for handling login authentication flow
 * Manages both login and registration states and validation
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repo: PatientRepository,
    private val application: Application
) : ViewModel() {
    private var errorClearJob: Job? = null

    // IDs from the database - StateFlow for reactive data streams
    val allIds: StateFlow<List<String>> = repo.getAllPatients()
        .map { patients -> patients.map { it.userId } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _registeredIds = MutableStateFlow<List<String>>(emptyList())
    val registeredIds: StateFlow<List<String>> = _registeredIds.asStateFlow()

    private val _unregisteredIds = MutableStateFlow<List<String>>(emptyList())
    val unregisteredIds: StateFlow<List<String>> = _unregisteredIds.asStateFlow()

    // UI state management - LiveData for UI state
    private val _loginState = MutableLiveData<LoginState>(LoginState.Initial)
    val loginState: LiveData<LoginState> = _loginState

    private val _registrationState = MutableLiveData<RegistrationState>(RegistrationState.Initial)
    val registrationState: LiveData<RegistrationState> = _registrationState

    // Form state management - StateFlow for form state with complex transformations
    private val _formState = MutableStateFlow(RegisterFormState())
    val formState: StateFlow<RegisterFormState> = _formState.asStateFlow()

    // Simple UI state elements
    private val _clinicianId = MutableLiveData("")
    val clinicianId: LiveData<String> = _clinicianId

    val passwordVisible: StateFlow<Boolean> = _formState
        .map { it.passwordVisible }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isDropdownExpanded = MutableLiveData(false)
    val isDropdownExpanded: LiveData<Boolean> = _isDropdownExpanded

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _password = MutableLiveData("")
    val password: LiveData<String> = _password

    private var isOperationInProgress = false

    // Jobs for cancellation handling
    private var loginJob: Job? = null
    private var registrationJob: Job? = null
    private var phoneValidationJob: Job? = null

    // Global error handler
    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        setErrorWithTimeout("An unexpected error occurred: ${throwable.localizedMessage}")
    }

    /**
     * Sets dropdown expansion state
     */
    fun setDropdownExpanded(expanded: Boolean) {
        _isDropdownExpanded.value = expanded
    }

    /**
     * Toggles password visibility
     */
    fun togglePasswordVisibility() {
        _formState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    /**
     * Selects a clinician ID from the dropdown
     */
    fun selectClinicianId(id: String) {
        _clinicianId.value = id
        _isDropdownExpanded.value = false
    }

    /**
     * Updates phone number with validation
     */
    fun updatePhoneNumber(phone: String) {
        _formState.update {
            it.copy(
                phoneNumber = phone,
                phoneValidation = FieldValidationState.NotValidated
            )
        }

        phoneValidationJob?.cancel()

        if (phone.length >= 10) {
            phoneValidationJob = viewModelScope.launch {
                delay(500)
                verifyPhoneNumber()
            }
        }
    }

    /**
     * Updates username with validation
     */
    fun updateName(name: String) {
        _formState.update {
            it.copy(username = name)
        }
        validateName()
    }

    /**
     * Updates password with validation
     */
    fun updatePassword(password: String) {
        _password.value = password

        _formState.update {
            it.copy(password = password)
        }
        validatePassword()

        if (_formState.value.confirmPassword.isNotEmpty()) {
            validateConfirmPassword()
        }
    }

    /**
     * Updates confirmation password with validation
     */
    fun updateConfirmPassword(confirmPassword: String) {
        _formState.update {
            it.copy(confirmPassword = confirmPassword)
        }
        validateConfirmPassword()
    }

    /**
     * Validates username field
     */
    private fun validateName() {
        val name = _formState.value.username
        val validationState = when {
            name.isBlank() -> FieldValidationState.Empty
            else -> FieldValidationState.Valid
        }

        _formState.update {
            it.copy(usernameValidation = validationState)
        }
    }

    /**
     * Validates password field
     */
    private fun validatePassword() {
        val password = _formState.value.password
        val validationState = when {
            password.isBlank() -> FieldValidationState.Empty
            password.length < 6 ||
                    !password.any { it.isDigit() } ||
                    !password.any { it.isLetter() } -> {
                FieldValidationState.Invalid("Password must be at least 6 characters long and contain letters and numbers")
            }
            else -> FieldValidationState.Valid
        }

        _formState.update {
            it.copy(passwordValidation = validationState)
        }
    }

    /**
     * Validates confirm password field
     */
    private fun validateConfirmPassword() {
        val password = _formState.value.password
        val confirmPassword = _formState.value.confirmPassword

        val validationState = when {
            confirmPassword.isBlank() -> FieldValidationState.Empty
            confirmPassword != password ->
                FieldValidationState.Invalid("Passwords do not match")
            else -> FieldValidationState.Valid
        }

        _formState.update {
            it.copy(confirmPasswordValidation = validationState)
        }
    }

    /**
     * Verifies phone number against patient record
     */
    fun verifyPhoneNumber() {
        val phone = _formState.value.phoneNumber

        if (phone.isBlank()) {
            _formState.update {
                it.copy(phoneValidation = FieldValidationState.Empty)
            }
            setErrorWithTimeout("Please enter your phone number")
            _registrationState.value = RegistrationState.Error("Please enter your phone number")
            return
        }

        val clinicianId = _clinicianId.value
        if (clinicianId.isNullOrBlank()) {
            setErrorWithTimeout("Please select a Clinician ID first")
            _registrationState.value = RegistrationState.Error("Please select a Clinician ID first")
            return
        }

        viewModelScope.launch(errorHandler) {
            _registrationState.value = RegistrationState.VerifyingPhone
            try {
                val patient = repo.getPatientById(clinicianId)

                if (patient != null) {
                    if (patient.phoneNumber != phone) {
                        _formState.update {
                            it.copy(phoneValidation = FieldValidationState.Invalid("Phone number does not match"))
                        }
                        _registrationState.value = RegistrationState.Error("Phone number does not match")
                        return@launch
                    }
                }

                _formState.update {
                    it.copy(phoneValidation = FieldValidationState.Valid)
                }
                _registrationState.value = RegistrationState.PhoneVerified
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error("Failed to verify phone: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Performs login authentication
     */
    fun login() {
        loginJob?.cancel()

        val clinicianId = _clinicianId.value
        val password = _password.value

        if (clinicianId.isNullOrBlank()) {
            setErrorWithTimeout("Please select your Clinician ID")
            _loginState.value = LoginState.Error("Please select your Clinician ID")
            return
        }

        if (password.isNullOrBlank()) {
            setErrorWithTimeout("Please enter your password")
            _loginState.value = LoginState.Error("Please enter your password")
            return
        }

        loginJob = viewModelScope.launch(errorHandler) {
            _loginState.value = LoginState.Authenticating(true)

            try {
                val patient = repo.getPatientById(clinicianId)
                    ?: throw Exception("Unknown user")

                if (patient.passwordHash.isEmpty()) {
                    _loginState.value = LoginState.NeedsRegistration
                    return@launch
                }

                if (hash(password) != patient.passwordHash) {
                    _loginState.value = LoginState.Error("Wrong password")
                    return@launch
                }

                // Login with user
                AuthenticationManager.loginWith(patient, application)

                _loginState.value = LoginState.Success(patient.userId)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Login failed")
            }
        }
    }

    /**
     * Performs user registration
     */
    fun register() {
        registrationJob?.cancel()

        val currentState = _formState.value
        val clinicianId = _clinicianId.value

        if (clinicianId.isNullOrBlank()) {
            setErrorWithTimeout("Please select a Clinician ID first")
            _registrationState.value = RegistrationState.Error("Please select a Clinician ID first")
            return
        }

        // Phone validation
        if (currentState.phoneNumber.isBlank()) {
            setErrorWithTimeout("Please enter your phone number")
            _registrationState.value = RegistrationState.Error("Please enter your phone number")
            return
        }

        if (currentState.phoneValidation != FieldValidationState.Valid) {
            verifyPhoneNumber()
            if (_formState.value.phoneValidation != FieldValidationState.Valid) {
                return
            }
        }

        // Name validation
        if (currentState.username.isBlank()) {
            _formState.update { it.copy(usernameValidation = FieldValidationState.Empty) }
            setErrorWithTimeout("Please enter your name")
            _registrationState.value = RegistrationState.Error("Please enter your name")
            return
        }

        // Password validation
        validatePassword()
        val passwordValidation = _formState.value.passwordValidation
        if (passwordValidation is FieldValidationState.Empty) {
            setErrorWithTimeout("Please enter a password")
            _registrationState.value = RegistrationState.Error("Please enter a password")
            return
        } else if (passwordValidation is FieldValidationState.Invalid) {
            setErrorWithTimeout(passwordValidation.message)
            _registrationState.value = RegistrationState.Error(passwordValidation.message)
            return
        }

        // Confirm password validation
        validateConfirmPassword()
        val confirmPasswordValidation = _formState.value.confirmPasswordValidation
        if (confirmPasswordValidation is FieldValidationState.Empty) {
            setErrorWithTimeout("Please confirm your password")
            _registrationState.value = RegistrationState.Error("Please confirm your password")
            return
        } else if (confirmPasswordValidation is FieldValidationState.Invalid) {
            setErrorWithTimeout(confirmPasswordValidation.message)
            _registrationState.value = RegistrationState.Error(confirmPasswordValidation.message)
            return
        }

        // Submit registration
        registrationJob = viewModelScope.launch(errorHandler) {
            _registrationState.value = RegistrationState.Submitting
            try {
                val old = repo.getPatientById(clinicianId)
                if (old == null) {
                    _registrationState.value = RegistrationState.Error("Patient not found")
                    return@launch
                }

                val updated = old.copy(
                    name = currentState.username,
                    passwordHash = hash(currentState.password)
                )
                repo.insertPatient(updated)
                AuthenticationManager.loginWith(updated, application)
                _registrationState.value = RegistrationState.Success(updated.userId)
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error("Registration error: ${e.message}")
            }
        }
    }

    /**
     * Verifies if the provided password matches the current user's password
     * @param oldPassword The password to verify
     * @return True if password matches, false otherwise
     */
    suspend fun verifyPassword(oldPassword: String): Boolean {
        val clinicianId = _clinicianId.value ?: return false

        try {
            val patient = repo.getPatientById(clinicianId) ?: return false
            return hash(oldPassword) == patient.passwordHash
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Updates user password
     * @param newPassword New password to set
     */
    fun updateUserInfo(newPassword: String? = null) {
        if (isOperationInProgress) {
            return
        }

        viewModelScope.launch(errorHandler) {
            try {
                isOperationInProgress = true

                val clinicianId = _clinicianId.value
                if (clinicianId.isNullOrBlank()) {
                    setErrorWithTimeout("Please select a Clinician ID first")
                    return@launch
                }

                val patient = repo.getPatientById(clinicianId)

                if (patient == null) {
                    setErrorWithTimeout("User not found")
                    return@launch
                }

                if (newPassword != null) {
                    // Update password
                    val updatedPatient = patient.copy(
                        passwordHash = hash(newPassword)
                    )
                    repo.insertPatient(updatedPatient)
                    setErrorWithTimeout("Password updated successfully", 2000)
                }

            } catch (e: Exception) {
                setErrorWithTimeout("Failed to update: ${e.message}")
            } finally {
                isOperationInProgress = false
            }
        }
    }

    /**
     * Sets error message with automatic timeout
     */
    fun setErrorWithTimeout(message: String, timeoutMs: Long = 3000) {
        _errorMessage.value = message
        errorClearJob?.cancel()
        errorClearJob = viewModelScope.launch {
            delay(timeoutMs)
            _errorMessage.value = null
        }
    }

    /**
     * Resets error states
     */
    fun resetError() {
        when (_loginState.value) {
            is LoginState.Error -> _loginState.value = LoginState.Initial
            else -> {}
        }

        when (_registrationState.value) {
            is RegistrationState.Error -> _registrationState.value = RegistrationState.Initial
            else -> {}
        }

        _errorMessage.value = null
        errorClearJob?.cancel()
    }

    /**
     * Resets all form fields
     */
    fun resetFields() {
        _password.value = ""
        _formState.value = RegisterFormState()
    }

    /**
     * Navigates to register screen
     */
    fun navigateToRegister() {
        _loginState.value = LoginState.NeedsRegistration
    }

    /**
     * Navigates to login screen
     */
    fun navigateToLogin() {
        _registrationState.value = RegistrationState.Initial
        _loginState.value = LoginState.Initial
    }

    /**
     * Simple SHA-256 hashing function for password security
     */
    private fun hash(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    override fun onCleared() {
        super.onCleared()
        loginJob?.cancel()
        registrationJob?.cancel()
        phoneValidationJob?.cancel()
        errorClearJob?.cancel()
    }

    init {
        viewModelScope.launch {
            repo.getAllPatients().collect { patients ->
                val allIds = patients.map { it.userId }
                val registered = patients.filter { it.passwordHash.isNotBlank() }.map { it.userId }
                val unregistered = allIds - registered.toSet()
                _registeredIds.value = registered
                _unregisteredIds.value = unregistered
            }
        }
    }
}