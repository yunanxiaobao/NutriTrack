package com.songshawn.nutritrack.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.songshawn.nutritrack.data.room.entity.NutriCoachTipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NutriCoachTipDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTip(tip: NutriCoachTipEntity): Long

    @Query("SELECT * FROM nutricoach_tips WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTipsByUserId(userId: String): Flow<List<NutriCoachTipEntity>>

    @Query("SELECT * FROM nutricoach_tips WHERE id = :tipId")
    suspend fun getTipById(tipId: Long): NutriCoachTipEntity?

    @Query("DELETE FROM nutricoach_tips WHERE id = :tipId")
    suspend fun deleteTipById(tipId: Long)

    @Query("DELETE FROM nutricoach_tips WHERE userId = :userId")
    suspend fun deleteAllTipsForUser(userId: String)

    @Query("SELECT COUNT(*) FROM nutricoach_tips WHERE userId = :userId")
    suspend fun getTipCountForUser(userId: String): Int
}