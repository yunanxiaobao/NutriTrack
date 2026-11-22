package com.songshawn.nutritrack.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.songshawn.nutritrack.data.room.entity.DailyScoreEntity
import kotlinx.coroutines.flow.Flow

/**
 * Projection returned by SQL aggregate queries on [DailyScoreEntity].
 */
data class DailyScoreAggregate(
    val dayCount: Int,
    val avgTotalScore: Double,
    val minTotalScore: Double,
    val maxTotalScore: Double,
    val avgCaloriesKcal: Double,
)

@Dao
interface DailyScoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyScore(score: DailyScoreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(scores: List<DailyScoreEntity>)

    @Query("SELECT * FROM daily_scores WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getDailyScore(userId: String, date: String): DailyScoreEntity?

    @Query("SELECT * FROM daily_scores WHERE userId = :userId AND date = :date LIMIT 1")
    fun getDailyScoreFlow(userId: String, date: String): Flow<DailyScoreEntity?>

    @Query("SELECT * FROM daily_scores WHERE userId = :userId ORDER BY date DESC LIMIT 1")
    fun getLatestDailyScoreFlow(userId: String): Flow<DailyScoreEntity?>

    @Query("SELECT * FROM daily_scores WHERE userId = :userId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestDailyScore(userId: String): DailyScoreEntity?

    /**
     * Returns up to [limit] most recent daily scores for [userId], ordered chronologically (oldest to newest)
     * so trend charts can plot left-to-right.
     */
    @Query(
        """
        SELECT * FROM (
            SELECT * FROM daily_scores
            WHERE userId = :userId
            ORDER BY date DESC
            LIMIT :limit
        )
        ORDER BY date ASC
        """,
    )
    fun getRecentDailyScoresFlow(userId: String, limit: Int): Flow<List<DailyScoreEntity>>

    @Query(
        """
        SELECT
            COUNT(*) AS dayCount,
            COALESCE(AVG(heifaTotalScore), 0.0) AS avgTotalScore,
            COALESCE(MIN(heifaTotalScore), 0.0) AS minTotalScore,
            COALESCE(MAX(heifaTotalScore), 0.0) AS maxTotalScore,
            COALESCE(AVG(totalCaloriesKcal), 0.0) AS avgCaloriesKcal
        FROM (
            SELECT heifaTotalScore, totalCaloriesKcal
            FROM daily_scores
            WHERE userId = :userId
            ORDER BY date DESC
            LIMIT :limit
        )
        """,
    )
    fun getRecentScoreAggregateFlow(userId: String, limit: Int): Flow<DailyScoreAggregate>

    @Query("DELETE FROM daily_scores WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
