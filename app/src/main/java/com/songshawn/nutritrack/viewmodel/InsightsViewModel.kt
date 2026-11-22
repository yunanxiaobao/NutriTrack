package com.songshawn.nutritrack.viewmodel

import androidx.lifecycle.ViewModel
import com.songshawn.nutritrack.data.repository.MealLoggingRepository
import com.songshawn.nutritrack.data.repository.PatientRepository
import com.songshawn.nutritrack.data.room.dao.DailyScoreAggregate
import com.songshawn.nutritrack.data.room.entity.DailyScoreEntity
import com.songshawn.nutritrack.data.room.entity.PatientEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * ViewModel for the Insights screen exposing current category breakdown and 7/30-day HEIFA score trends.
 */
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val mealLoggingRepository: MealLoggingRepository,
) : ViewModel() {

    /**
     * Reactive flow of the user's current score breakdown: prefers the latest computed
     * [DailyScoreEntity] from logged meals and falls back to the legacy [PatientEntity] row.
     */
    fun getPatient(userId: String): Flow<PatientEntity?> = combine(
        patientRepository.getPatientFlow(userId),
        mealLoggingRepository.getLatestDailyScoreFlow(userId),
    ) { patient, latestDaily ->
        when {
            latestDaily != null && patient != null -> latestDaily.toPatientEntity(
                displayName = patient.name,
                gender = patient.sex ?: com.songshawn.nutritrack.data.models.Gender.FEMALE,
                phoneNumber = patient.phoneNumber,
            )
            latestDaily != null -> latestDaily.toPatientEntity(displayName = "NutriTrack User")
            else -> patient
        }
    }

    fun getRecentDailyScores(userId: String, days: Int): Flow<List<DailyScoreEntity>> =
        mealLoggingRepository.getRecentDailyScoresFlow(userId, days)

    fun getRecentScoreAggregate(userId: String, days: Int): Flow<DailyScoreAggregate> =
        mealLoggingRepository.getRecentScoreAggregateFlow(userId, days)
}
