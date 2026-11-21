package com.songshawn.nutritrack.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.songshawn.nutritrack.data.room.entity.PatientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(patients: List<PatientEntity>)

    @Update
    suspend fun updatePatient(patient: PatientEntity)

    @Query("SELECT * FROM patient WHERE name != '' AND passwordHash != ''")
    fun getRegisteredPatients(): Flow<List<PatientEntity>>

    @Query("SELECT COUNT(*) FROM patient WHERE name != '' AND passwordHash != ''")
    suspend fun getRegisteredCount(): Int

    @Query("SELECT * FROM patient WHERE userId = :userId")
    suspend fun getPatient(userId: String): PatientEntity?

    @Query("SELECT * FROM patient")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patient WHERE userId = :userId LIMIT 1")
    suspend fun getPatientById(userId: String): PatientEntity?

    @Query("SELECT * FROM patient WHERE userId = :userId LIMIT 1")
    fun getPatientFlow(userId: String): Flow<PatientEntity?>

    @Query("DELETE FROM patient")
    suspend fun deleteAllPatients()

    @Query("SELECT COUNT(*) FROM patient")
    suspend fun getCount(): Int
}