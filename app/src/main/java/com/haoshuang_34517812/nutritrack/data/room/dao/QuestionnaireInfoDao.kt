package com.haoshuang_34517812.nutritrack.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.haoshuang_34517812.nutritrack.data.room.entity.QuestionnaireInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionnaireInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInfo(info: QuestionnaireInfoEntity)

    @Query("SELECT * FROM food_intake WHERE patientId = :userId")
    suspend fun getInfoByUser(userId: String): QuestionnaireInfoEntity?

    @Query("DELETE FROM food_intake WHERE patientId = :userId")
    suspend fun deleteInfoByUser(userId: String)

    @Query("SELECT * FROM food_intake WHERE patientId = :userId LIMIT 1")
    fun getInfoByUserFlow(userId: String): Flow<QuestionnaireInfoEntity?>
}