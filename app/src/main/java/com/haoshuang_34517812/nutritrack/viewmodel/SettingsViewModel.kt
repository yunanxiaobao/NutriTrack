package com.haoshuang_34517812.nutritrack.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshuang_34517812.nutritrack.data.models.UserInfo
import com.haoshuang_34517812.nutritrack.data.repository.PatientRepository
import com.haoshuang_34517812.nutritrack.util.AuthenticationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

/**
 * ViewModel for managing settings screen functionality
 * Handles user information, logout, and account operations
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PatientRepository
) : ViewModel() {

    // UI state management
    private val _uiState = MutableLiveData<SettingsUiState>(SettingsUiState.Initial)
    val uiState: LiveData<SettingsUiState> = _uiState

    // User ID
    private val _userId = MutableLiveData<String>()
    val userId: LiveData<String> = _userId

    // User information
    private val _userInfo = MutableLiveData<UserInfo>()
    val userInfo: LiveData<UserInfo> = _userInfo

    // Operation status flag
    private var isOperationInProgress = false

    /**
     * Sets the current user ID and loads user information
     * @param id User ID to set
     */
    fun setUserId(id: String) {
        if (_userId.value == id) {
            return
        }
        _userId.value = id
        loadUserInfo(id)
    }

    /**
     * Loads user information from repository
     * @param userId ID of the user to load
     */
    private fun loadUserInfo(userId: String) {
        if (isOperationInProgress) {
            return
        }

        viewModelScope.launch {
            try {
                isOperationInProgress = true
                _uiState.value = SettingsUiState.Loading

                val patient = repository.getPatientById(userId)
                if (patient != null) {
                    _userInfo.value = UserInfo(
                        name = patient.name,
                        phoneNumber = patient.phoneNumber,
                        userId = patient.userId,
                        sex = patient.sex.toString()
                    )
                    _uiState.value = SettingsUiState.Success
                } else {
                    // Firebase accounts have no legacy patient row; fall back
                    // to the Firebase profile until cloud profile sync lands.
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    if (firebaseUser != null) {
                        _userInfo.value = UserInfo(
                            name = firebaseUser.displayName ?: "NutriTrack user",
                            phoneNumber = "",
                            userId = firebaseUser.uid,
                            sex = ""
                        )
                        _uiState.value = SettingsUiState.Success
                    } else {
                        _uiState.value = SettingsUiState.Error("User not found")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("Failed to load user info: ${e.message}")
            } finally {
                isOperationInProgress = false
            }
        }
    }

    /**
     * Logs out the current user
     * @param context Context needed for authentication manager
     */
    fun logout(context: Context) {
        if (isOperationInProgress || _uiState.value is SettingsUiState.Logout) {
            return
        }

        isOperationInProgress = true

        try {
            AuthenticationManager.logout(context)
            _uiState.value = SettingsUiState.Logout
        } catch (e: Exception) {
            _uiState.value = SettingsUiState.Error("Logout failed: ${e.message}")
        } finally {
            isOperationInProgress = false
        }
    }

    /**
     * Deletes user account (clears credentials but keeps records)
     */
    fun deleteAccount() {
        if (isOperationInProgress) {
            return
        }

        viewModelScope.launch {
            try {
                isOperationInProgress = true
                _uiState.value = SettingsUiState.Loading

                val userId = _userId.value
                if (userId.isNullOrEmpty()) {
                    _uiState.value = SettingsUiState.Error("User ID not available")
                    return@launch
                }

                val patient = repository.getPatientById(userId)

                if (patient != null) {
                    // Clear credentials but keep user records and scores
                    val updatedPatient = patient.copy(
                        passwordHash = "",
                        name = "",
                        phoneNumber = ""
                    )
                    repository.insertPatient(updatedPatient)
                    _uiState.value = SettingsUiState.AccountDeleted
                } else {
                    _uiState.value = SettingsUiState.Error("User not found")
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("Failed to delete account: ${e.message}")
            } finally {
                isOperationInProgress = false
            }
        }
    }

    /**
     * Updates user information
     * @param name Optional new name
     * @param phone Optional new phone number
     */
    fun updateUserInfo(name: String? = null, phone: String? = null) {
        if (isOperationInProgress) {
            return
        }

        viewModelScope.launch {
            try {
                isOperationInProgress = true

                val userId = _userId.value
                if (userId.isNullOrEmpty()) {
                    _uiState.value = SettingsUiState.Error("User ID not available")
                    return@launch
                }

                val patient = repository.getPatientById(userId)

                if (patient == null) {
                    _uiState.value = SettingsUiState.Error("User not found")
                    return@launch
                }

                val updated = patient.copy(
                    name = name ?: patient.name,
                    phoneNumber = phone ?: patient.phoneNumber
                )
                repository.updatePatient(updated)

                // Update UI state with new info
                _userInfo.value = UserInfo(
                    name = updated.name,
                    phoneNumber = updated.phoneNumber,
                    userId = updated.userId,
                    sex = updated.sex.toString()
                )

                _uiState.value = SettingsUiState.Success
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("Update failed: ${e.message}")
            } finally {
                isOperationInProgress = false
            }
        }
    }
}