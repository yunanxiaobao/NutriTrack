package com.songshawn.nutritrack.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.songshawn.nutritrack.data.room.entity.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOutboxItem(item: SyncOutboxEntity)

    @Query("SELECT * FROM sync_outbox WHERE attemptCount < :maxAttempts ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingBatch(maxAttempts: Int = 8, limit: Int = 25): List<SyncOutboxEntity>

    @Query("SELECT COUNT(*) FROM sync_outbox")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_outbox")
    suspend fun getPendingCount(): Int

    @Query("DELETE FROM sync_outbox WHERE documentUuid = :documentUuid")
    suspend fun deleteByUuid(documentUuid: String)

    @Query(
        "UPDATE sync_outbox SET attemptCount = attemptCount + 1, lastError = :error " +
            "WHERE documentUuid = :documentUuid",
    )
    suspend fun recordFailedAttempt(documentUuid: String, error: String?)
}
