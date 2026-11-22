package com.songshawn.nutritrack.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.songshawn.nutritrack.data.repository.CloudSyncRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * WorkManager background worker that drains the Room `sync_outbox` table to Firestore
 * with network connectivity constraints and exponential backoff on transient failures.
 */
class OutboxSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface OutboxWorkerEntryPoint {
        fun cloudSyncRepository(): CloudSyncRepository
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            OutboxWorkerEntryPoint::class.java,
        )
        return try {
            val allSucceeded = entryPoint.cloudSyncRepository().drainOutboxBatch()
            if (allSucceeded) Result.success() else Result.retry()
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            if (runAttemptCount < MAX_WORKER_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "nutritrack_outbox_cloud_sync"
        private const val MAX_WORKER_ATTEMPTS = 6
        private const val INITIAL_BACKOFF_SECONDS = 15L

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<OutboxSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    INITIAL_BACKOFF_SECONDS,
                    TimeUnit.SECONDS,
                )
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
