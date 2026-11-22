package com.songshawn.nutritrack.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.songshawn.nutritrack.data.models.Gender
import com.songshawn.nutritrack.data.room.entity.PatientEntity
import kotlinx.coroutines.flow.Flow

/**
 * SQL aggregate projection grouping patient HEIFA scores by gender.
 */
data class GenderScoreSummary(
    val sex: Gender,
    val patientCount: Int,
    val avgTotalScore: Double,
    val avgVegetableScore: Double,
    val avgFruitScore: Double,
)

@Dao
interface PatientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(patients: List<PatientEntity>)

    @Update
    suspend fun updatePatient(patient: PatientEntity)

    @Query("SELECT * FROM patient WHERE name != ''")
    fun getRegisteredPatients(): Flow<List<PatientEntity>>

    @Query("SELECT COUNT(*) FROM patient WHERE name != ''")
    suspend fun getRegisteredCount(): Int

    @Query("SELECT * FROM patient WHERE userId = :userId")
    suspend fun getPatient(userId: String): PatientEntity?

    @Query("SELECT * FROM patient")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patient WHERE userId = :userId LIMIT 1")
    suspend fun getPatientById(userId: String): PatientEntity?

    @Query("SELECT * FROM patient WHERE userId = :userId LIMIT 1")
    fun getPatientFlow(userId: String): Flow<PatientEntity?>

    /**
     * SQL GROUP BY / AVG aggregate query used by the Clinician dashboard.
     */
    @Query(
        """
        SELECT
            sex AS sex,
            COUNT(*) AS patientCount,
            COALESCE(AVG(heifaTotalScore), 0.0) AS avgTotalScore,
            COALESCE(AVG(vegetableScore), 0.0) AS avgVegetableScore,
            COALESCE(AVG(fruitScore), 0.0) AS avgFruitScore
        FROM patient
        WHERE sex IS NOT NULL
        GROUP BY sex
        """,
    )
    suspend fun getGenderScoreSummaries(): List<GenderScoreSummary>

    @Query("DELETE FROM patient")
    suspend fun deleteAllPatients()

    @Query("SELECT COUNT(*) FROM patient")
    suspend fun getCount(): Int
}
