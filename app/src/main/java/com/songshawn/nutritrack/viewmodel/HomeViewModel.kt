package com.songshawn.nutritrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songshawn.nutritrack.data.repository.CloudSyncRepository
import com.songshawn.nutritrack.data.repository.MealLoggingRepository
import com.songshawn.nutritrack.data.repository.PatientRepository
import com.songshawn.nutritrack.util.AuthenticationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class HomeUiState(
    val userId: String = "",
    val username: String = "",
    val totalScore: Int = 0,
    val hasScores: Boolean = true,
)

/**
 * ViewModel for the Home screen displaying user profile and live HEIFA scores via [StateFlow].
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PatientRepository,
    private val mealLoggingRepository: MealLoggingRepository,
    private val cloudSync: CloudSyncRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadPatientData(userId: String) {
        _uiState.update { it.copy(userId = userId) }
        viewModelScope.launch {
            mealLoggingRepository.ensureCatalogSeeded()
            cloudSync.syncUserData(userId)
            val latestDaily = mealLoggingRepository.getLatestDailyScore(userId)
            val patient = repository.getPatientById(userId)

            val resolvedScore = latestDaily?.heifaTotalScore ?: patient?.heifaTotalScore
            val resolvedName = patient?.name?.takeIf { it.isNotBlank() }
                ?: AuthenticationManager.getCurrentDisplayName()
                ?: "friend"

            _uiState.update {
                it.copy(
                    userId = userId,
                    username = resolvedName,
                    totalScore = (resolvedScore ?: 0.0).roundToInt(),
                    hasScores = resolvedScore != null,
                )
            }
        }
    }
}
