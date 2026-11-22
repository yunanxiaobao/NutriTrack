package com.songshawn.nutritrack.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Durable transactional outbox entry representing a pending Firestore write.
 *
 * Keyed by a deterministic [documentUuid] so repeated enqueues or WorkManager retries are
 * idempotent both locally and in Firestore.
 */
@Entity(
    tableName = "sync_outbox",
    indices = [Index(value = ["userId", "createdAt"])],
)
data class SyncOutboxEntity(
    @PrimaryKey
    val documentUuid: String,
    val userId: String,
    /** e.g. "QUESTIONNAIRE", "TIP" */
    val entityType: String,
    /** Target Firestore document path such as `users/{uid}/tips/{documentUuid}`. */
    val firestoreDocumentPath: String,
    val payloadJson: String,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        fun deterministicUuid(namespaceKey: String): String =
            UUID.nameUUIDFromBytes(namespaceKey.toByteArray(Charsets.UTF_8)).toString()
    }
}
